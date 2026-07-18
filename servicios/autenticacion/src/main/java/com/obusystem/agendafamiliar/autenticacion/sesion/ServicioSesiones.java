package com.obusystem.agendafamiliar.autenticacion.sesion;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obusystem.agendafamiliar.autenticacion.usuario.RepositorioUsuarios;
import com.obusystem.agendafamiliar.autenticacion.usuario.Usuario;

@Service
public class ServicioSesiones {
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private final RepositorioUsuarios usuarios;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder claves;
    private final JwtEncoder jwtEncoder;
    private final String emisor;
    private final Duration duracionAccess;
    private final Duration duracionRefresh;

    public ServicioSesiones(
            RepositorioUsuarios usuarios,
            JdbcTemplate jdbc,
            PasswordEncoder claves,
            JwtEncoder jwtEncoder,
            @Value("${seguridad.jwt.emisor}") String emisor,
            @Value("${seguridad.jwt.duracion-minutos}") long duracionMinutos,
            @Value("${seguridad.refresh.duracion-dias}") long duracionDias) {
        this.usuarios = usuarios;
        this.jdbc = jdbc;
        this.claves = claves;
        this.jwtEncoder = jwtEncoder;
        this.emisor = emisor;
        this.duracionAccess = Duration.ofMinutes(duracionMinutos);
        this.duracionRefresh = Duration.ofDays(duracionDias);
    }

    @Transactional
    public PaqueteSesion iniciar(SolicitudInicioSesion solicitud) {
        Usuario usuario = usuarios.findByCorreoIgnoreCase(solicitud.correo())
                .filter(encontrado -> "ACTIVO".equals(encontrado.getEstado()))
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!claves.matches(solicitud.clave(), usuario.getClaveHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }
        return crearSesion(usuario);
    }

    @Transactional
    public PaqueteSesion renovar(String refreshToken, String csrfCookie, String csrfHeader) {
        validarCsrfBasico(csrfCookie, csrfHeader);
        String tokenHash = hash(refreshToken);
        List<SesionEncontrada> resultados = jdbc.query("""
                SELECT sr.id, sr.csrf_hash, u.id
                FROM sesiones_refresh sr
                JOIN usuarios u ON u.id = sr.usuario_id
                WHERE sr.token_hash = ? AND sr.revocado_en IS NULL
                  AND sr.expira_en > NOW() AND u.estado = 'ACTIVO'
                FOR UPDATE
                """, (rs, fila) -> new SesionEncontrada(
                        rs.getObject(1, UUID.class), rs.getString(2), rs.getLong(3)), tokenHash);
        if (resultados.isEmpty()) {
            throw new BadCredentialsException("Sesión vencida o revocada");
        }

        SesionEncontrada anterior = resultados.getFirst();
        if (!comparacionConstante(anterior.csrfHash(), hash(csrfHeader))) {
            throw new ExcepcionCsrf();
        }
        Usuario usuario = usuarios.findById(anterior.usuarioIdInterno())
                .orElseThrow(() -> new BadCredentialsException("Sesión inválida"));
        PaqueteSesion nueva = crearSesion(usuario);
        jdbc.update("UPDATE sesiones_refresh SET revocado_en = NOW(), ultimo_uso_en = NOW(), reemplazado_por = ? WHERE id = ?",
                nueva.sesionId(), anterior.sesionId());
        return nueva;
    }

    @Transactional
    public void cerrar(String refreshToken, String csrfCookie, String csrfHeader) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        validarCsrfBasico(csrfCookie, csrfHeader);
        int actualizadas = jdbc.update("""
                UPDATE sesiones_refresh SET revocado_en = COALESCE(revocado_en, NOW()), ultimo_uso_en = NOW()
                WHERE token_hash = ? AND csrf_hash = ?
                """, hash(refreshToken), hash(csrfHeader));
        if (actualizadas == 0) {
            throw new ExcepcionCsrf();
        }
    }

    @Transactional(readOnly = true)
    public RespuestaUsuario usuarioActual(UUID idPublico) {
        Usuario usuario = usuarios.findByIdPublico(idPublico)
                .filter(encontrado -> "ACTIVO".equals(encontrado.getEstado()))
                .orElseThrow(() -> new BadCredentialsException("Usuario inactivo"));
        return new RespuestaUsuario(usuario.getIdPublico(), usuario.getCorreo());
    }

    private PaqueteSesion crearSesion(Usuario usuario) {
        Instant ahora = Instant.now();
        Instant expiraAccess = ahora.plus(duracionAccess);
        Instant expiraRefresh = ahora.plus(duracionRefresh);
        String refreshToken = tokenAleatorio();
        String csrfToken = tokenAleatorio();
        UUID sesionId = UUID.randomUUID();

        jdbc.update("""
                INSERT INTO sesiones_refresh (id, usuario_id, token_hash, csrf_hash, expira_en)
                VALUES (?, ?, ?, ?, ?)
                """, sesionId, usuario.getId(), hash(refreshToken), hash(csrfToken),
                OffsetDateTime.ofInstant(expiraRefresh, ZoneOffset.UTC));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .issuedAt(ahora)
                .expiresAt(expiraAccess)
                .subject(usuario.getIdPublico().toString())
                .claim("correo", usuario.getCorreo())
                .build();
        JwsHeader encabezado = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(encabezado, claims)).getTokenValue();
        RespuestaSesion respuesta = new RespuestaSesion(accessToken, expiraAccess, usuario.getIdPublico(), usuario.getCorreo());
        return new PaqueteSesion(sesionId, respuesta, refreshToken, csrfToken, expiraRefresh);
    }

    private static void validarCsrfBasico(String cookie, String header) {
        if (cookie == null || header == null || !comparacionConstante(cookie, header)) {
            throw new ExcepcionCsrf();
        }
    }

    private static boolean comparacionConstante(String izquierda, String derecha) {
        return MessageDigest.isEqual(izquierda.getBytes(StandardCharsets.UTF_8), derecha.getBytes(StandardCharsets.UTF_8));
    }

    private static String tokenAleatorio() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return BASE64_URL.encodeToString(bytes);
    }

    private static String hash(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new BadCredentialsException("Sesión inexistente");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException("SHA-256 no está disponible", imposible);
        }
    }

    public record PaqueteSesion(UUID sesionId, RespuestaSesion respuesta, String refreshToken,
            String csrfToken, Instant expiraRefresh) { }

    private record SesionEncontrada(UUID sesionId, String csrfHash, long usuarioIdInterno) { }
}
