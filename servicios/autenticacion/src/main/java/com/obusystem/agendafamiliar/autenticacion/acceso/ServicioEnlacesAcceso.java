package com.obusystem.agendafamiliar.autenticacion.acceso;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.obusystem.agendafamiliar.autenticacion.usuario.RepositorioUsuarios;
import com.obusystem.agendafamiliar.autenticacion.usuario.Usuario;

@Service
public class ServicioEnlacesAcceso {
    private static final String ROL_ADMIN = "ADMINISTRADOR_PLATAFORMA";
    private static final UUID PAPA_FAMILIA_TEST = UUID.fromString("0197f100-0000-7000-8000-000000000101");
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();
    private final JdbcTemplate jdbc;
    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder claves;
    private final SecretKey firma;

    public ServicioEnlacesAcceso(JdbcTemplate jdbc, RepositorioUsuarios usuarios,
            PasswordEncoder claves, SecretKey firma) {
        this.jdbc = jdbc;
        this.usuarios = usuarios;
        this.claves = claves;
        this.firma = firma;
    }

    @Transactional
    public RespuestaEnlace.Generado invitar(String claveIdempotencia, SolicitudInvitacion solicitud, Jwt jwt) {
        UUID actor = autorizar(jwt);
        validarClave(claveIdempotencia);
        String correo = solicitud.correo().trim().toLowerCase(Locale.ROOT);
        bloquear(actor, claveIdempotencia);
        List<FilaEnlace> anterior = porIdempotencia(actor, claveIdempotencia);
        if (!anterior.isEmpty()) return generado(anterior.getFirst());
        if (usuarios.findByCorreoIgnoreCase(correo).isPresent() || usuarios.findByIdPublico(solicitud.usuarioId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya existe; usa restablecer acceso");
        }
        jdbc.update("UPDATE enlaces_acceso SET revocado_en=NOW() WHERE tipo='INVITACION' AND (usuario_publico_id=? OR correo_normalizado=?) AND consumido_en IS NULL AND revocado_en IS NULL",
                solicitud.usuarioId(), correo);
        return crear("INVITACION", actor, claveIdempotencia, solicitud.usuarioId(), solicitud.familiaId(),
                solicitud.familiaNombre().trim(), correo, Duration.ofHours(48));
    }

    @Transactional
    public RespuestaEnlace.Generado restablecer(UUID usuarioId, String claveIdempotencia, Jwt jwt) {
        UUID actor = autorizar(jwt);
        validarClave(claveIdempotencia);
        Usuario usuario = usuarios.findByIdPublico(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cuenta no encontrada"));
        bloquear(actor, claveIdempotencia);
        List<FilaEnlace> anterior = porIdempotencia(actor, claveIdempotencia);
        if (!anterior.isEmpty()) return generado(anterior.getFirst());
        jdbc.update("UPDATE enlaces_acceso SET revocado_en=NOW() WHERE tipo='RESTABLECIMIENTO' AND usuario_publico_id=? AND consumido_en IS NULL AND revocado_en IS NULL",
                usuarioId);
        return crear("RESTABLECIMIENTO", actor, claveIdempotencia, usuarioId, null, null,
                usuario.getCorreo(), Duration.ofMinutes(30));
    }

    @Transactional(readOnly = true)
    public RespuestaEnlace.Lista consultar(UUID familiaId, Jwt jwt) {
        autorizar(jwt);
        return new RespuestaEnlace.Lista(jdbc.query("""
                SELECT id, tipo, usuario_publico_id, correo_normalizado, expira_en, creado_en,
                    CASE WHEN consumido_en IS NOT NULL THEN 'USADO'
                         WHEN revocado_en IS NOT NULL THEN 'REVOCADO'
                         WHEN expira_en <= NOW() THEN 'VENCIDO' ELSE 'PENDIENTE' END estado
                FROM enlaces_acceso WHERE familia_publica_id=? ORDER BY creado_en DESC LIMIT 200
                """, (rs, fila) -> new RespuestaEnlace.Administrado(
                        rs.getObject("id", UUID.class), rs.getString("tipo"),
                        rs.getObject("usuario_publico_id", UUID.class), rs.getString("correo_normalizado"),
                        rs.getString("estado"), rs.getTimestamp("expira_en").toInstant(),
                        rs.getTimestamp("creado_en").toInstant()), familiaId));
    }

    @Transactional(readOnly = true)
    public RespuestaEnlace.Cuentas consultarCuentas(List<UUID> ids, Jwt jwt) {
        autorizar(jwt);
        if (ids.size() > 200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Demasiadas cuentas solicitadas");
        return new RespuestaEnlace.Cuentas(ids.stream().distinct().map(usuarios::findByIdPublico)
                .flatMap(java.util.Optional::stream)
                .map(usuario -> new RespuestaEnlace.Cuenta(usuario.getIdPublico(), usuario.getCorreo(), usuario.getEstado()))
                .toList());
    }

    @Transactional
    public void revocar(UUID enlaceId, Jwt jwt) {
        UUID actor = autorizar(jwt);
        int cambio = jdbc.update("UPDATE enlaces_acceso SET revocado_en=NOW() WHERE id=? AND consumido_en IS NULL AND revocado_en IS NULL", enlaceId);
        if (cambio == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Enlace activo no encontrado");
        auditar(actor, "REVOCAR", enlaceId, "Enlace de acceso revocado");
    }

    @Transactional(readOnly = true)
    public RespuestaEnlace.Publico consultarPublico(String token) {
        FilaConsumo enlace = enlaceValido(token, false);
        return new RespuestaEnlace.Publico(enlace.tipo(), ocultar(enlace.correo()), enlace.familiaNombre(), enlace.expiraEn());
    }

    @Transactional
    public void consumir(SolicitudConsumirEnlace solicitud) {
        FilaConsumo enlace = enlaceValido(solicitud.token(), true);
        if ("INVITACION".equals(enlace.tipo())) {
            if (usuarios.findByCorreoIgnoreCase(enlace.correo()).isPresent()
                    || usuarios.findByIdPublico(enlace.usuarioId()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya fue activada");
            }
            usuarios.saveAndFlush(new Usuario(enlace.usuarioId(), enlace.correo(), claves.encode(solicitud.clave())));
        } else {
            Usuario usuario = usuarios.findByIdPublico(enlace.usuarioId())
                    .orElseThrow(() -> enlaceInvalido());
            usuario.activarConClave(claves.encode(solicitud.clave()));
            usuarios.saveAndFlush(usuario);
            jdbc.update("UPDATE sesiones_refresh SET revocado_en=COALESCE(revocado_en, NOW()) WHERE usuario_id=?",
                    usuario.getId());
            if (ROL_ADMIN.equals(usuario.getRolPlataforma()) && !PAPA_FAMILIA_TEST.equals(usuario.getIdPublico())) {
                jdbc.update("UPDATE usuarios SET rol_plataforma='USUARIO', actualizado_en=NOW(), version=version+1 WHERE id_publico=? AND rol_plataforma=?",
                        PAPA_FAMILIA_TEST, ROL_ADMIN);
            }
        }
        jdbc.update("UPDATE enlaces_acceso SET consumido_en=NOW() WHERE id=?", enlace.id());
        auditar(enlace.usuarioId(), "CONSUMIR", enlace.id(), "Enlace de acceso utilizado");
    }

    private RespuestaEnlace.Generado crear(String tipo, UUID actor, String claveIdempotencia,
            UUID usuarioId, UUID familiaId, String familiaNombre, String correo, Duration duracion) {
        UUID id = UUID.randomUUID();
        String token = token(id);
        Instant expira = Instant.now().plus(duracion);
        jdbc.update("""
                INSERT INTO enlaces_acceso (id, tipo, actor_publico_id, clave_idempotencia, usuario_publico_id,
                    familia_publica_id, familia_nombre, correo_normalizado, token_hash, expira_en)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, tipo, actor, claveIdempotencia, usuarioId, familiaId, familiaNombre, correo,
                hash(token), OffsetDateTime.ofInstant(expira, ZoneOffset.UTC));
        auditar(actor, "CREAR", id, "Enlace de acceso generado");
        return new RespuestaEnlace.Generado(id, tipo, usuarioId, "/activar#token=" + token, expira);
    }

    private List<FilaEnlace> porIdempotencia(UUID actor, String clave) {
        return jdbc.query("SELECT id, tipo, usuario_publico_id, expira_en FROM enlaces_acceso WHERE actor_publico_id=? AND clave_idempotencia=?",
                (rs, fila) -> new FilaEnlace(rs.getObject("id", UUID.class), rs.getString("tipo"),
                        rs.getObject("usuario_publico_id", UUID.class), rs.getTimestamp("expira_en").toInstant()), actor, clave);
    }

    private RespuestaEnlace.Generado generado(FilaEnlace fila) {
        return new RespuestaEnlace.Generado(fila.id(), fila.tipo(), fila.usuarioId(),
                "/activar#token=" + token(fila.id()), fila.expiraEn());
    }

    private FilaConsumo enlaceValido(String token, boolean bloquear) {
        String sufijo = bloquear ? " FOR UPDATE" : "";
        List<FilaConsumo> enlaces = jdbc.query("""
                SELECT id, tipo, usuario_publico_id, correo_normalizado, familia_nombre, expira_en
                FROM enlaces_acceso
                WHERE token_hash=? AND consumido_en IS NULL AND revocado_en IS NULL AND expira_en>NOW()
                """ + sufijo, (rs, fila) -> new FilaConsumo(rs.getObject("id", UUID.class), rs.getString("tipo"),
                        rs.getObject("usuario_publico_id", UUID.class), rs.getString("correo_normalizado"),
                        rs.getString("familia_nombre"), rs.getTimestamp("expira_en").toInstant()), hash(token));
        if (enlaces.isEmpty()) throw enlaceInvalido();
        return enlaces.getFirst();
    }

    private ResponseStatusException enlaceInvalido() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "El enlace no existe, venció o ya fue utilizado");
    }

    private UUID autorizar(Jwt jwt) {
        if (jwt == null || !ROL_ADMIN.equals(jwt.getClaimAsString("rol_plataforma"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso reservado a administración de plataforma");
        }
        try { return UUID.fromString(jwt.getSubject()); }
        catch (RuntimeException error) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identidad administrativa inválida"); }
    }

    private void bloquear(UUID actor, String clave) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", (rs, fila) -> 0, actor + ":" + clave);
    }

    private void validarClave(String clave) {
        if (clave == null || clave.isBlank() || clave.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key inválida");
        }
    }

    private String token(UUID id) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(firma);
            mac.update("agenda-enlace-v1:".getBytes(StandardCharsets.UTF_8));
            byte[] firmaToken = mac.doFinal(ByteBuffer.allocate(16)
                    .putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array());
            return id + "." + BASE64_URL.encodeToString(firmaToken);
        } catch (Exception error) {
            throw new IllegalStateException("No se pudo firmar el enlace", error);
        }
    }

    private static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 no está disponible", imposible);
        }
    }

    private static String ocultar(String correo) {
        int arroba = correo.indexOf('@');
        return arroba <= 0 ? "***" : correo.substring(0, 1) + "***" + correo.substring(arroba);
    }

    private void auditar(UUID actor, String operacion, UUID id, String resumen) {
        jdbc.update("INSERT INTO auditoria_acceso (actor_publico_id, operacion, entidad_publica_id, resumen_seguro) VALUES (?, ?, ?, ?)",
                actor, operacion, id, resumen);
    }

    private record FilaEnlace(UUID id, String tipo, UUID usuarioId, Instant expiraEn) { }
    private record FilaConsumo(UUID id, String tipo, UUID usuarioId, String correo,
            String familiaNombre, Instant expiraEn) { }
}
