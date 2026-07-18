package com.obusystem.agendafamiliar.agenda.agenda;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
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
public class ServicioAccionesAgenda {
    private final AccesoFamilia acceso;
    private final JdbcTemplate jdbc;

    public ServicioAccionesAgenda(AccesoFamilia acceso, JdbcTemplate jdbc) {
        this.acceso = acceso;
        this.jdbc = jdbc;
    }

    @Transactional
    public RespuestaAccionAgenda actuar(UUID familiaId, String entidad, UUID entidadId, AccionAgenda accion,
            String clave, SolicitudAccionAgenda solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        String tipo = normalizarEntidad(entidad);
        validarSolicitud(accion, solicitud);
        UUID actor = UUID.fromString(jwt.getSubject());
        int reservada = jdbc.update("INSERT INTO acciones_agenda (familia_id, clave_idempotencia, entidad, entidad_publica_id, accion, actor_publico_id) VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (familia_id, clave_idempotencia) DO NOTHING",
                familia.getId(), clave, tipo, entidadId, accion.name(), actor);
        if (reservada == 0) return respuestaExistente(familia.getId(), clave, tipo, entidadId, accion);
        return tipo.equals("TAREA")
                ? actuarTarea(familia.getId(), entidadId, accion, clave, solicitud, actor)
                : actuarEvento(familia.getId(), entidadId, accion, clave, solicitud, actor);
    }

    private RespuestaAccionAgenda actuarTarea(Long familiaId, UUID id, AccionAgenda accion, String clave,
            SolicitudAccionAgenda solicitud, UUID actor) {
        Fila fila = fila("SELECT id, fecha_limite fecha FROM tareas WHERE familia_id=? AND id_publico=?", familiaId, id);
        if (accion == AccionAgenda.REPROGRAMAR) {
            UUID nueva = UuidV7.nuevo();
            int cambio = jdbc.update("UPDATE tareas SET estado='REPROGRAMADA', actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id_publico=? AND estado='PENDIENTE'",
                    familiaId, id);
            exigirPendiente(cambio);
            jdbc.update("INSERT INTO tareas (id_publico, familia_id, perfil_id, titulo, descripcion, fecha_limite, estado, recurrencia_id, tarea_origen_id) SELECT ?, familia_id, perfil_id, titulo, descripcion, ?, 'PENDIENTE', recurrencia_id, id FROM tareas WHERE id=? AND familia_id=?",
                    nueva, Timestamp.from(solicitud.fechaNueva()), fila.id(), familiaId);
            finalizar(familiaId, clave, fila.fecha(), solicitud.fechaNueva(), nueva);
            auditar(familiaId, actor, "REPROGRAMAR", "TAREA", id, "Tarea reprogramada conservando la instancia anterior");
            return new RespuestaAccionAgenda(nueva, id, "TAREA", "PENDIENTE", solicitud.fechaNueva());
        }
        String estado = accion == AccionAgenda.COMPLETAR ? "COMPLETADA" : "OMITIDA";
        int cambio = jdbc.update("UPDATE tareas SET estado=?, actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id_publico=? AND estado='PENDIENTE'",
                estado, familiaId, id);
        exigirPendiente(cambio);
        finalizar(familiaId, clave, fila.fecha(), fila.fecha(), id);
        auditar(familiaId, actor, accion.name(), "TAREA", id, "Tarea marcada como " + estado.toLowerCase());
        return new RespuestaAccionAgenda(id, null, "TAREA", estado, fila.fecha());
    }

    private RespuestaAccionAgenda actuarEvento(Long familiaId, UUID id, AccionAgenda accion, String clave,
            SolicitudAccionAgenda solicitud, UUID actor) {
        if (accion == AccionAgenda.COMPLETAR) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un evento no se completa");
        Fila fila = fila("SELECT id, inicio_en fecha FROM eventos WHERE familia_id=? AND id_publico=?", familiaId, id);
        if (accion == AccionAgenda.REPROGRAMAR) {
            UUID nuevo = UuidV7.nuevo();
            int cambio = jdbc.update("UPDATE eventos SET estado='REPROGRAMADO', actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id_publico=? AND estado='PROGRAMADO'",
                    familiaId, id);
            exigirPendiente(cambio);
            jdbc.update("INSERT INTO eventos (id_publico, familia_id, perfil_id, titulo, tipo, lugar, direccion, notas, inicio_en, fin_en, estado, lugar_guardado_id, recurrencia_id, evento_origen_id) SELECT ?, familia_id, perfil_id, titulo, tipo, lugar, direccion, notas, ?, CASE WHEN fin_en IS NULL THEN NULL ELSE ? + (fin_en - inicio_en) END, 'PROGRAMADO', lugar_guardado_id, recurrencia_id, id FROM eventos WHERE id=? AND familia_id=?",
                    nuevo, Timestamp.from(solicitud.fechaNueva()), Timestamp.from(solicitud.fechaNueva()), fila.id(), familiaId);
            finalizar(familiaId, clave, fila.fecha(), solicitud.fechaNueva(), nuevo);
            auditar(familiaId, actor, "REPROGRAMAR", "EVENTO", id, "Evento reprogramado conservando la instancia anterior");
            return new RespuestaAccionAgenda(nuevo, id, "EVENTO", "PROGRAMADO", solicitud.fechaNueva());
        }
        int cambio = jdbc.update("UPDATE eventos SET estado='OMITIDO', actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id_publico=? AND estado='PROGRAMADO'",
                familiaId, id);
        exigirPendiente(cambio);
        finalizar(familiaId, clave, fila.fecha(), fila.fecha(), id);
        auditar(familiaId, actor, "OMITIR", "EVENTO", id, "Evento omitido");
        return new RespuestaAccionAgenda(id, null, "EVENTO", "OMITIDO", fila.fecha());
    }

    private Fila fila(String sql, Long familiaId, UUID id) {
        List<Fila> filas = jdbc.query(sql, (rs, numero) -> new Fila(rs.getLong("id"), rs.getTimestamp("fecha").toInstant()), familiaId, id);
        if (filas.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Elemento no encontrado");
        return filas.getFirst();
    }

    private RespuestaAccionAgenda respuestaExistente(Long familiaId, String clave, String entidad, UUID id,
            AccionAgenda accion) {
        List<AccionGuardada> acciones = jdbc.query("SELECT entidad, entidad_publica_id, accion, resultado_publico_id, fecha_nueva FROM acciones_agenda WHERE familia_id=? AND clave_idempotencia=?",
                (rs, fila) -> new AccionGuardada(rs.getString("entidad"), rs.getObject("entidad_publica_id", UUID.class),
                        rs.getString("accion"), rs.getObject("resultado_publico_id", UUID.class),
                        rs.getTimestamp("fecha_nueva") == null ? null : rs.getTimestamp("fecha_nueva").toInstant()), familiaId, clave);
        AccionGuardada guardada = acciones.getFirst();
        if (!guardada.entidad().equals(entidad) || !guardada.entidadId().equals(id) || !guardada.accion().equals(accion.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave idempotente ya fue usada para otra acción");
        }
        String estado = accion == AccionAgenda.REPROGRAMAR ? (entidad.equals("TAREA") ? "PENDIENTE" : "PROGRAMADO")
                : accion == AccionAgenda.COMPLETAR ? "COMPLETADA" : entidad.equals("TAREA") ? "OMITIDA" : "OMITIDO";
        return new RespuestaAccionAgenda(guardada.resultadoId(), accion == AccionAgenda.REPROGRAMAR ? id : null,
                entidad, estado, guardada.fecha());
    }

    private void finalizar(Long familiaId, String clave, Instant anterior, Instant nueva, UUID resultado) {
        jdbc.update("UPDATE acciones_agenda SET fecha_anterior=?, fecha_nueva=?, resultado_publico_id=? WHERE familia_id=? AND clave_idempotencia=?",
                Timestamp.from(anterior), Timestamp.from(nueva), resultado, familiaId, clave);
    }

    private void exigirPendiente(int cambio) {
        if (cambio == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "El elemento ya fue resuelto por otra acción");
    }

    private void validarSolicitud(AccionAgenda accion, SolicitudAccionAgenda solicitud) {
        if (accion == AccionAgenda.REPROGRAMAR && (solicitud == null || solicitud.fechaNueva() == null
                || !solicitud.fechaNueva().isAfter(Instant.now()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar una nueva fecha futura");
        }
    }

    private String normalizarEntidad(String entidad) {
        return switch (entidad.toLowerCase()) {
            case "tareas" -> "TAREA";
            case "eventos" -> "EVENTO";
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de agenda no encontrado");
        };
    }

    private void auditar(Long familiaId, UUID actor, String operacion, String entidad, UUID entidadId, String resumen) {
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, ?, ?, ?, ?)",
                familiaId, actor, operacion, entidad, entidadId, resumen);
    }

    private record Fila(Long id, Instant fecha) { }
    private record AccionGuardada(String entidad, UUID entidadId, String accion, UUID resultadoId, Instant fecha) { }
}
