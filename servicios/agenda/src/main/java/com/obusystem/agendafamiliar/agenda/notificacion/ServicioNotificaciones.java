package com.obusystem.agendafamiliar.agenda.notificacion;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Time;
import java.time.Instant;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Base64;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.obusystem.agendafamiliar.agenda.family.AccesoFamilia;
import com.obusystem.agendafamiliar.agenda.family.Familia;
import com.obusystem.agendafamiliar.agenda.util.UuidV7;

@Service
public class ServicioNotificaciones {
    private final AccesoFamilia acceso;
    private final JdbcTemplate jdbc;
    private final ServicioWebPush webPush;

    public ServicioNotificaciones(AccesoFamilia acceso, JdbcTemplate jdbc, ServicioWebPush webPush) {
        this.acceso = acceso;
        this.jdbc = jdbc;
        this.webPush = webPush;
    }

    @Transactional
    public RespuestaNotificaciones consultar(UUID familiaPublicaId, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaPublicaId, jwt);
        UUID usuarioId = usuario(jwt);
        asegurarPreferencias(familia.getId(), usuarioId);
        List<RespuestaNotificaciones.Aviso> avisos = jdbc.query(
                "SELECT id_publico, tipo, titulo, detalle, destino, creada_en, leida_en "
                        + "FROM notificaciones WHERE familia_id=? AND usuario_publico_id=? "
                        + "ORDER BY creada_en DESC LIMIT 60",
                (rs, fila) -> new RespuestaNotificaciones.Aviso(
                        rs.getObject("id_publico", UUID.class), rs.getString("tipo"), rs.getString("titulo"),
                        rs.getString("detalle"), rs.getString("destino"), rs.getTimestamp("creada_en").toInstant(),
                        rs.getTimestamp("leida_en") == null ? null : rs.getTimestamp("leida_en").toInstant()),
                familia.getId(), usuarioId);
        Long sinLeer = jdbc.queryForObject("SELECT COUNT(*) FROM notificaciones WHERE familia_id=? AND usuario_publico_id=? AND leida_en IS NULL",
                Long.class, familia.getId(), usuarioId);
        return new RespuestaNotificaciones(avisos, sinLeer == null ? 0 : sinLeer,
                leerPreferencias(familia.getId(), usuarioId), leerDispositivos(familia.getId(), usuarioId),
                webPush.disponible(), webPush.clavePublica());
    }

    @Transactional
    public void marcarLeida(UUID familiaPublicaId, UUID notificacionId, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaPublicaId, jwt);
        int cambios = jdbc.update("UPDATE notificaciones SET leida_en=COALESCE(leida_en, NOW()) "
                        + "WHERE familia_id=? AND usuario_publico_id=? AND id_publico=?",
                familia.getId(), usuario(jwt), notificacionId);
        if (cambios == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aviso no encontrado");
    }

    @Transactional
    public void marcarTodasLeidas(UUID familiaPublicaId, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaPublicaId, jwt);
        jdbc.update("UPDATE notificaciones SET leida_en=NOW() WHERE familia_id=? AND usuario_publico_id=? AND leida_en IS NULL",
                familia.getId(), usuario(jwt));
    }

    @Transactional
    public RespuestaNotificaciones.Preferencias guardarPreferencias(UUID familiaPublicaId,
            SolicitudPreferenciasNotificacion solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaPublicaId, jwt);
        UUID usuarioId = usuario(jwt);
        jdbc.update("INSERT INTO preferencias_notificacion (familia_id, usuario_publico_id, tareas, eventos, salud, botiquin, silencio_desde, silencio_hasta) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (familia_id, usuario_publico_id) DO UPDATE SET "
                        + "tareas=EXCLUDED.tareas, eventos=EXCLUDED.eventos, salud=EXCLUDED.salud, botiquin=EXCLUDED.botiquin, "
                        + "silencio_desde=EXCLUDED.silencio_desde, "
                        + "silencio_hasta=EXCLUDED.silencio_hasta, actualizado_en=NOW()",
                familia.getId(), usuarioId, solicitud.tareas(), solicitud.eventos(), solicitud.salud(),
                solicitud.botiquin(), Time.valueOf(solicitud.silencioDesde()),
                Time.valueOf(solicitud.silencioHasta()));
        auditar(familia.getId(), usuarioId, "PREFERENCIAS", "Preferencias de avisos actualizadas");
        return leerPreferencias(familia.getId(), usuarioId);
    }

    @Transactional
    public RespuestaNotificaciones.Dispositivo registrarDispositivo(UUID familiaPublicaId,
            SolicitudSuscripcionPush solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaPublicaId, jwt);
        validarEndpoint(solicitud.endpoint());
        validarClaves(solicitud.claveP256dh(), solicitud.claveAuth());
        UUID usuarioId = usuario(jwt);
        UUID idPublico = UuidV7.nuevo();
        String hash = hash(solicitud.endpoint());
        UUID guardado = jdbc.queryForObject("INSERT INTO suscripciones_push (id_publico, familia_id, usuario_publico_id, endpoint, endpoint_hash, clave_p256dh, clave_auth, dispositivo) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (familia_id, usuario_publico_id, endpoint_hash) DO UPDATE SET "
                        + "endpoint=EXCLUDED.endpoint, clave_p256dh=EXCLUDED.clave_p256dh, clave_auth=EXCLUDED.clave_auth, "
                        + "dispositivo=EXCLUDED.dispositivo, activa=TRUE, ultimo_error_en=NULL RETURNING id_publico",
                UUID.class, idPublico, familia.getId(), usuarioId, solicitud.endpoint().trim(), hash,
                solicitud.claveP256dh().trim(), solicitud.claveAuth().trim(), solicitud.dispositivo().trim());
        auditar(familia.getId(), usuarioId, "ACTIVAR_PUSH", "Avisos activados en un dispositivo");
        return leerDispositivo(familia.getId(), usuarioId, guardado);
    }

    @Transactional
    public void revocarDispositivo(UUID familiaPublicaId, UUID dispositivoId, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaPublicaId, jwt);
        UUID usuarioId = usuario(jwt);
        int cambios = jdbc.update("UPDATE suscripciones_push SET activa=FALSE WHERE familia_id=? AND usuario_publico_id=? AND id_publico=? AND activa",
                familia.getId(), usuarioId, dispositivoId);
        if (cambios == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo no encontrado");
        auditar(familia.getId(), usuarioId, "REVOCAR_PUSH", "Avisos desactivados en un dispositivo");
    }

    private void asegurarPreferencias(Long familiaId, UUID usuarioId) {
        jdbc.update("INSERT INTO preferencias_notificacion (familia_id, usuario_publico_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
                familiaId, usuarioId);
    }

    private RespuestaNotificaciones.Preferencias leerPreferencias(Long familiaId, UUID usuarioId) {
        return jdbc.queryForObject("SELECT tareas, eventos, salud, botiquin, silencio_desde, silencio_hasta "
                        + "FROM preferencias_notificacion WHERE familia_id=? AND usuario_publico_id=?",
                (rs, fila) -> new RespuestaNotificaciones.Preferencias(rs.getBoolean("tareas"), rs.getBoolean("eventos"),
                        rs.getBoolean("salud"), rs.getBoolean("botiquin"),
                        rs.getTime("silencio_desde").toLocalTime(), rs.getTime("silencio_hasta").toLocalTime()),
                familiaId, usuarioId);
    }

    private List<RespuestaNotificaciones.Dispositivo> leerDispositivos(Long familiaId, UUID usuarioId) {
        return jdbc.query("SELECT id_publico, dispositivo, activa, creado_en, ultimo_exito_en FROM suscripciones_push "
                        + "WHERE familia_id=? AND usuario_publico_id=? ORDER BY creado_en DESC",
                (rs, fila) -> dispositivo(rs.getObject("id_publico", UUID.class), rs.getString("dispositivo"),
                        rs.getBoolean("activa"), rs.getTimestamp("creado_en").toInstant(),
                        rs.getTimestamp("ultimo_exito_en") == null ? null : rs.getTimestamp("ultimo_exito_en").toInstant()),
                familiaId, usuarioId);
    }

    private RespuestaNotificaciones.Dispositivo leerDispositivo(Long familiaId, UUID usuarioId, UUID idPublico) {
        return jdbc.queryForObject("SELECT id_publico, dispositivo, activa, creado_en, ultimo_exito_en FROM suscripciones_push "
                        + "WHERE familia_id=? AND usuario_publico_id=? AND id_publico=?",
                (rs, fila) -> dispositivo(rs.getObject("id_publico", UUID.class), rs.getString("dispositivo"),
                        rs.getBoolean("activa"), rs.getTimestamp("creado_en").toInstant(),
                        rs.getTimestamp("ultimo_exito_en") == null ? null : rs.getTimestamp("ultimo_exito_en").toInstant()),
                familiaId, usuarioId, idPublico);
    }

    private RespuestaNotificaciones.Dispositivo dispositivo(UUID id, String nombre, boolean activo,
            Instant creadoEn, Instant ultimoExitoEn) {
        return new RespuestaNotificaciones.Dispositivo(id, nombre, activo, creadoEn, ultimoExitoEn);
    }

    private void validarEndpoint(String valor) {
        try {
            URI uri = URI.create(valor.trim());
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443) || !hostPushPermitido(host)) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La suscripción del dispositivo no es válida");
        }
    }

    private boolean hostPushPermitido(String host) {
        return host.equals("fcm.googleapis.com") || host.endsWith(".push.services.mozilla.com")
                || host.endsWith(".push.apple.com") || host.endsWith(".notify.windows.com");
    }

    private void validarClaves(String p256dh, String auth) {
        try {
            byte[] publica = Base64.getUrlDecoder().decode(p256dh.trim());
            byte[] autenticacion = Base64.getUrlDecoder().decode(auth.trim());
            if (publica.length != 65 || autenticacion.length < 16 || autenticacion.length > 32) {
                throw new IllegalArgumentException();
            }
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las claves del dispositivo no son válidas");
        }
    }

    private String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException imposible) {
            throw new IllegalStateException(imposible);
        }
    }

    private UUID usuario(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private void auditar(Long familiaId, UUID usuarioId, String operacion, String resumen) {
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, ?, 'NOTIFICACION', ?, ?)",
                familiaId, usuarioId, operacion, UuidV7.nuevo(), resumen);
    }
}
