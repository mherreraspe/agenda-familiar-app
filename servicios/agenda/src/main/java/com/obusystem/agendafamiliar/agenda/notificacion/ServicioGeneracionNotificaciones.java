package com.obusystem.agendafamiliar.agenda.notificacion;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.obusystem.agendafamiliar.agenda.family.ContextoFamilia;

@Service
public class ServicioGeneracionNotificaciones {
    private final JdbcTemplate jdbc;
    private final ContextoFamilia contexto;
    private final TransactionTemplate transacciones;
    private final ServicioWebPush webPush;

    public ServicioGeneracionNotificaciones(JdbcTemplate jdbc, ContextoFamilia contexto,
            TransactionTemplate transacciones, ServicioWebPush webPush) {
        this.jdbc = jdbc;
        this.contexto = contexto;
        this.transacciones = transacciones;
        this.webPush = webPush;
    }

    @Scheduled(fixedDelayString = "${notificaciones.generacion-intervalo-ms:60000}",
            initialDelayString = "${notificaciones.generacion-retardo-inicial-ms:60000}")
    public void generarPendientes() {
        List<FamiliaProgramada> familias = jdbc.query("SELECT id, zona_horaria FROM familias",
                (rs, fila) -> new FamiliaProgramada(rs.getLong("id"), rs.getString("zona_horaria")));
        for (FamiliaProgramada familia : familias) {
            transacciones.executeWithoutResult(estado -> {
                contexto.activar(familia.id());
                generarFamilia(familia.id());
                enviarFamilia(familia.id(), familia.zonaHoraria());
            });
        }
    }

    void generarFamilia(Long familiaId) {
        generarTareas(familiaId);
        generarEventos(familiaId, "24H", "23 hours 55 minutes", "24 hours 5 minutes", "Cita o evento mañana");
        generarEventos(familiaId, "1H", "55 minutes", "1 hour 5 minutes", "Cita o evento dentro de una hora");
        generarSalud(familiaId);
        generarBotiquin(familiaId);
    }

    private void generarTareas(Long familiaId) {
        jdbc.update("INSERT INTO notificaciones (id_publico, familia_id, usuario_publico_id, tipo, entidad, entidad_publica_id, clave_deduplicacion, titulo, detalle, destino) "
                + "SELECT gen_random_uuid(), t.familia_id, mf.usuario_publico_id, 'TAREA', 'TAREA', t.id_publico, "
                + "'tarea:' || t.id_publico, 'Tarea pendiente', t.titulo, '/hoy?aviso=' || t.id_publico "
                + "FROM tareas t JOIN perfiles p ON p.id=t.perfil_id "
                + "JOIN miembros_familia mf ON mf.familia_id=t.familia_id AND mf.activo "
                + "AND (p.usuario_publico_id IS NULL OR mf.usuario_publico_id=p.usuario_publico_id) "
                + "LEFT JOIN preferencias_notificacion pn ON pn.familia_id=t.familia_id AND pn.usuario_publico_id=mf.usuario_publico_id "
                + "WHERE t.familia_id=? AND t.estado='PENDIENTE' AND t.avisar AND t.fecha_limite BETWEEN NOW() - INTERVAL '5 minutes' AND NOW() + INTERVAL '1 minute' "
                + "AND COALESCE(pn.tareas, TRUE) ON CONFLICT DO NOTHING", familiaId);
    }

    private void generarEventos(Long familiaId, String momento, String desde, String hasta, String titulo) {
        jdbc.update("INSERT INTO notificaciones (id_publico, familia_id, usuario_publico_id, tipo, entidad, entidad_publica_id, clave_deduplicacion, titulo, detalle, destino) "
                + "SELECT gen_random_uuid(), e.familia_id, mf.usuario_publico_id, 'EVENTO', 'EVENTO', e.id_publico, "
                + "'evento:' || e.id_publico || ':" + momento + "', ?, e.titulo, '/agenda?aviso=' || e.id_publico "
                + "FROM eventos e LEFT JOIN perfiles p ON p.id=e.perfil_id "
                + "JOIN miembros_familia mf ON mf.familia_id=e.familia_id AND mf.activo "
                + "AND (p.usuario_publico_id IS NULL OR mf.usuario_publico_id=p.usuario_publico_id) "
                + "LEFT JOIN preferencias_notificacion pn ON pn.familia_id=e.familia_id AND pn.usuario_publico_id=mf.usuario_publico_id "
                + "WHERE e.familia_id=? AND e.estado='PROGRAMADO' AND e.avisar_" + momento.toLowerCase() + " AND e.inicio_en BETWEEN NOW() + INTERVAL '" + desde + "' AND NOW() + INTERVAL '" + hasta + "' "
                + "AND COALESCE(pn.eventos, TRUE) ON CONFLICT DO NOTHING", titulo, familiaId);
    }

    private void generarSalud(Long familiaId) {
        jdbc.update("INSERT INTO notificaciones (id_publico, familia_id, usuario_publico_id, tipo, entidad, entidad_publica_id, clave_deduplicacion, titulo, detalle, destino) "
                + "SELECT gen_random_uuid(), o.familia_id, mf.usuario_publico_id, 'SALUD', 'OCURRENCIA', o.id_publico, "
                + "'salud:' || o.id_publico, 'Toma pendiente', t.nombre_libre || ' · ' || p.nombre_visible, '/salud?aviso=' || o.id_publico "
                + "FROM ocurrencias_tratamiento o JOIN tratamientos t ON t.id=o.tratamiento_id JOIN perfiles p ON p.id=t.perfil_id "
                + "LEFT JOIN perfiles rp ON rp.id=t.responsable_perfil_id "
                + "JOIN miembros_familia mf ON mf.familia_id=o.familia_id AND mf.activo "
                + "AND (COALESCE(rp.usuario_publico_id, p.usuario_publico_id) IS NULL OR mf.usuario_publico_id=COALESCE(rp.usuario_publico_id, p.usuario_publico_id)) "
                + "LEFT JOIN preferencias_notificacion pn ON pn.familia_id=o.familia_id AND pn.usuario_publico_id=mf.usuario_publico_id "
                + "WHERE o.familia_id=? AND o.estado='PENDIENTE' AND o.programada_en BETWEEN NOW() - INTERVAL '5 minutes' AND NOW() + INTERVAL '1 minute' "
                + "AND COALESCE(pn.salud, TRUE) ON CONFLICT DO NOTHING", familiaId);
    }

    private void generarBotiquin(Long familiaId) {
        jdbc.update("INSERT INTO notificaciones (id_publico, familia_id, usuario_publico_id, tipo, entidad, entidad_publica_id, clave_deduplicacion, titulo, detalle, destino) "
                + "SELECT gen_random_uuid(), l.familia_id, mf.usuario_publico_id, 'BOTIQUIN', 'LOTE_MEDICAMENTO', l.id_publico, "
                + "'botiquin:' || l.id_publico || ':' || CURRENT_DATE, 'Revisa el botiquín', m.nombre, '/salud?seccion=botiquin&aviso=' || l.id_publico "
                + "FROM lotes_medicamento l JOIN medicamentos m ON m.id=l.medicamento_id "
                + "JOIN miembros_familia mf ON mf.familia_id=l.familia_id AND mf.activo "
                + "LEFT JOIN preferencias_notificacion pn ON pn.familia_id=l.familia_id AND pn.usuario_publico_id=mf.usuario_publico_id "
                + "WHERE l.familia_id=? AND l.estado NOT IN ('AGOTADO','DESCARTADO') AND COALESCE(pn.botiquin, TRUE) "
                + "AND ((l.avisar_vencimiento AND l.fecha_vencimiento - l.anticipacion_vencimiento_dias = CURRENT_DATE) "
                + "OR (l.avisar_apertura AND l.estado_envase='ABIERTO' AND l.duracion_abierto_dias IS NOT NULL "
                + "AND l.abierto_en + l.duracion_abierto_dias::INTEGER - l.anticipacion_apertura_dias = CURRENT_DATE)) "
                + "ON CONFLICT DO NOTHING", familiaId);
    }

    private void enviarFamilia(Long familiaId, String zonaHoraria) {
        if (!webPush.disponible()) return;
        List<Entrega> entregas = jdbc.query("SELECT n.id notificacion_id, s.id suscripcion_id, s.endpoint, s.clave_p256dh, s.clave_auth, "
                        + "pn.silencio_desde, pn.silencio_hasta FROM notificaciones n "
                        + "JOIN suscripciones_push s ON s.familia_id=n.familia_id AND s.usuario_publico_id=n.usuario_publico_id AND s.activa "
                        + "JOIN miembros_familia mf ON mf.familia_id=s.familia_id AND mf.usuario_publico_id=s.usuario_publico_id AND mf.activo "
                        + "LEFT JOIN preferencias_notificacion pn ON pn.familia_id=n.familia_id AND pn.usuario_publico_id=n.usuario_publico_id "
                        + "WHERE n.familia_id=? AND n.enviada_push_en IS NULL AND n.creada_en >= NOW() - INTERVAL '1 day' "
                        + "AND (s.ultimo_error_en IS NULL OR s.ultimo_error_en <= NOW() - INTERVAL '15 minutes')",
                (rs, fila) -> new Entrega(rs.getLong("notificacion_id"), rs.getLong("suscripcion_id"),
                        rs.getString("endpoint"), rs.getString("clave_p256dh"), rs.getString("clave_auth"),
                        rs.getTime("silencio_desde") == null ? LocalTime.of(22, 0) : rs.getTime("silencio_desde").toLocalTime(),
                        rs.getTime("silencio_hasta") == null ? LocalTime.of(7, 0) : rs.getTime("silencio_hasta").toLocalTime()),
                familiaId);
        LocalTime ahora = LocalTime.now(ZoneId.of(zonaHoraria));
        for (Entrega entrega : entregas) {
            if (enSilencio(ahora, entrega.silencioDesde(), entrega.silencioHasta())) continue;
            ServicioWebPush.Resultado resultado = webPush.enviar(entrega.endpoint(), entrega.claveP256dh(), entrega.claveAuth());
            if (resultado == ServicioWebPush.Resultado.ENVIADO) {
                jdbc.update("UPDATE suscripciones_push SET ultimo_exito_en=NOW(), ultimo_error_en=NULL WHERE id=?", entrega.suscripcionId());
                jdbc.update("UPDATE notificaciones SET enviada_push_en=NOW() WHERE id=?", entrega.notificacionId());
            } else if (resultado == ServicioWebPush.Resultado.EXPIRADO) {
                jdbc.update("UPDATE suscripciones_push SET activa=FALSE, ultimo_error_en=NOW() WHERE id=?", entrega.suscripcionId());
            } else if (resultado == ServicioWebPush.Resultado.ERROR) {
                jdbc.update("UPDATE suscripciones_push SET ultimo_error_en=NOW() WHERE id=?", entrega.suscripcionId());
            }
        }
    }

    private boolean enSilencio(LocalTime ahora, LocalTime desde, LocalTime hasta) {
        if (desde.equals(hasta)) return false;
        if (desde.isBefore(hasta)) return !ahora.isBefore(desde) && ahora.isBefore(hasta);
        return !ahora.isBefore(desde) || ahora.isBefore(hasta);
    }

    private record FamiliaProgramada(Long id, String zonaHoraria) { }
    private record Entrega(Long notificacionId, Long suscripcionId, String endpoint, String claveP256dh,
            String claveAuth, LocalTime silencioDesde, LocalTime silencioHasta) { }
}
