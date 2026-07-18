package com.obusystem.agendafamiliar.agenda.tratamiento;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
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
public class ServicioOcurrencias {
    private final AccesoFamilia acceso;
    private final JdbcTemplate jdbc;

    public ServicioOcurrencias(AccesoFamilia acceso, JdbcTemplate jdbc) {
        this.acceso = acceso;
        this.jdbc = jdbc;
    }

    @Transactional
    public RespuestaOcurrencias consultar(UUID familiaId, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        materializar(familia);
        sincronizarRevision(familia.getId());
        return new RespuestaOcurrencias(consultarOcurrencias(familia.getId()), consultarRevision(familia.getId()));
    }

    @Transactional
    public void materializarTratamiento(Familia familia, Long tratamientoId) {
        materializar(familia, tratamientoId);
    }

    @Transactional
    public RespuestaOcurrencias.OcurrenciaResumen cambiarEstado(UUID familiaId, UUID ocurrenciaId,
            EstadoOcurrencia estado, String claveIdempotencia, SolicitudAccionOcurrencia solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        if (estado == EstadoOcurrencia.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La acción no puede devolver una ocurrencia a pendiente");
        }
        String clave = claveIdempotencia == null ? "" : claveIdempotencia.trim();
        if (clave.isEmpty() || clave.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key es obligatorio y admite hasta 120 caracteres");
        }

        List<AccionExistente> anteriores = jdbc.query(
                "SELECT o.id_publico, a.accion FROM acciones_ocurrencia a JOIN ocurrencias_tratamiento o ON o.id=a.ocurrencia_id WHERE a.familia_id=? AND a.clave_idempotencia=?",
                (rs, fila) -> new AccionExistente(rs.getObject(1, UUID.class), rs.getString(2)), familia.getId(), clave);
        if (!anteriores.isEmpty()) {
            AccionExistente anterior = anteriores.getFirst();
            if (!anterior.ocurrenciaId().equals(ocurrenciaId) || !anterior.accion().equals(estado.name())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave de idempotencia ya fue usada para otra acción");
            }
            return buscarOcurrencia(familia.getId(), ocurrenciaId);
        }

        OcurrenciaInterna ocurrencia = buscarInterna(familia.getId(), ocurrenciaId);
        if (!ocurrencia.estado().equals(EstadoOcurrencia.PENDIENTE.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La ocurrencia ya fue resuelta");
        }
        Instant pospuestaA = solicitud == null ? null : solicitud.pospuestaA();
        if ((estado == EstadoOcurrencia.POSPUESTA || estado == EstadoOcurrencia.REPROGRAMADA)
                && (pospuestaA == null || !pospuestaA.isAfter(Instant.now()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva fecha debe estar en el futuro");
        }
        UUID actor = actor(jwt);
        int actualizadas = jdbc.update("UPDATE ocurrencias_tratamiento SET estado=?, pospuesta_a=?, resuelta_por=?, resuelta_en=NOW(), actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id=? AND estado='PENDIENTE'",
                estado.name(), pospuestaA == null ? null : Timestamp.from(pospuestaA), actor, familia.getId(), ocurrencia.id());
        if (actualizadas != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "La ocurrencia cambió al mismo tiempo");
        try {
            jdbc.update("INSERT INTO acciones_ocurrencia (familia_id, ocurrencia_id, clave_idempotencia, accion, actor_publico_id) VALUES (?, ?, ?, ?, ?)",
                    familia.getId(), ocurrencia.id(), clave, estado.name(), actor);
        } catch (DuplicateKeyException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave de idempotencia ya fue usada");
        }
        if (estado == EstadoOcurrencia.POSPUESTA || estado == EstadoOcurrencia.REPROGRAMADA) {
            jdbc.update("INSERT INTO ocurrencias_tratamiento (id_publico, familia_id, tratamiento_id, ocurrencia_origen_id, programada_en) VALUES (?, ?, ?, ?, ?) ON CONFLICT (familia_id, tratamiento_id, programada_en) DO NOTHING",
                    UuidV7.nuevo(), familia.getId(), ocurrencia.tratamientoId(), ocurrencia.id(), Timestamp.from(pospuestaA));
        }
        jdbc.update("UPDATE elementos_revision SET estado='RESUELTO', resuelto_por=?, resuelto_en=NOW(), actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND origen='OCURRENCIA' AND entidad_publica_id=? AND estado='PENDIENTE'",
                actor, familia.getId(), ocurrenciaId);
        String resumen = "Ocurrencia marcada como " + estado.name().toLowerCase();
        if (pospuestaA != null) resumen += " para " + pospuestaA;
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, ?, 'OCURRENCIA_TRATAMIENTO', ?, ?)",
                familia.getId(), actor, estado.name(), ocurrenciaId, resumen);
        return buscarOcurrencia(familia.getId(), ocurrenciaId);
    }

    @Transactional
    public void cerrarTratamiento(UUID familiaId, UUID tratamientoId, String claveIdempotencia,
            SolicitudCierreTratamiento solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        String clave = validarClave(claveIdempotencia);
        List<AccionTratamientoExistente> anteriores = jdbc.query(
                "SELECT t.id_publico, a.accion FROM acciones_tratamiento a JOIN tratamientos t ON t.id=a.tratamiento_id WHERE a.familia_id=? AND a.clave_idempotencia=?",
                (rs, fila) -> new AccionTratamientoExistente(rs.getObject(1, UUID.class), rs.getString(2)),
                familia.getId(), clave);
        if (!anteriores.isEmpty()) {
            AccionTratamientoExistente anterior = anteriores.getFirst();
            if (!anterior.tratamientoId().equals(tratamientoId) || !anterior.accion().equals("CERRAR")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave de idempotencia ya fue usada para otra acción");
            }
            return;
        }
        List<TratamientoInterno> tratamientos = jdbc.query(
                "SELECT id, estado FROM tratamientos WHERE familia_id=? AND id_publico=?",
                (rs, fila) -> new TratamientoInterno(rs.getLong(1), rs.getString(2)), familia.getId(), tratamientoId);
        if (tratamientos.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tratamiento no encontrado");
        TratamientoInterno tratamiento = tratamientos.getFirst();
        if (!tratamiento.estado().equals("ACTIVO")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El tratamiento ya está cerrado");
        }
        UUID actor = actor(jwt);
        String motivo = solicitud == null ? null : limpiar(solicitud.motivo());
        int actualizados = jdbc.update("UPDATE tratamientos SET estado='CERRADO', fecha_fin=LEAST(COALESCE(fecha_fin, CURRENT_DATE), CURRENT_DATE), actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id=? AND estado='ACTIVO'",
                familia.getId(), tratamiento.id());
        if (actualizados != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "El tratamiento cambió al mismo tiempo");
        jdbc.update("UPDATE ocurrencias_tratamiento SET estado='CANCELADA', resuelta_por=?, resuelta_en=NOW(), actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND tratamiento_id=? AND estado='PENDIENTE'",
                actor, familia.getId(), tratamiento.id());
        try {
            jdbc.update("INSERT INTO acciones_tratamiento (familia_id, tratamiento_id, clave_idempotencia, accion, actor_publico_id, motivo) VALUES (?, ?, ?, 'CERRAR', ?, ?)",
                    familia.getId(), tratamiento.id(), clave, actor, motivo);
        } catch (DuplicateKeyException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave de idempotencia ya fue usada");
        }
        jdbc.update("UPDATE elementos_revision SET estado='RESUELTO', resuelto_por=?, resuelto_en=NOW(), actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND origen='TRATAMIENTO' AND entidad_publica_id=? AND estado='PENDIENTE'",
                actor, familia.getId(), tratamientoId);
        String resumen = motivo == null ? "Tratamiento cerrado" : "Tratamiento cerrado: " + motivo;
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, 'CERRAR', 'TRATAMIENTO', ?, ?)",
                familia.getId(), actor, tratamientoId, resumen);
    }

    @Transactional
    public void cerrarRevision(UUID familiaId, UUID elementoId, String claveIdempotencia, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        String clave = claveIdempotencia == null ? "" : claveIdempotencia.trim();
        if (clave.isEmpty() || clave.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key es obligatorio y admite hasta 120 caracteres");
        }
        Integer repetida = jdbc.queryForObject("SELECT COUNT(*) FROM acciones_revision WHERE familia_id=? AND clave_idempotencia=?",
                Integer.class, familia.getId(), clave);
        if (repetida != null && repetida > 0) return;
        List<RevisionInterna> elementos = jdbc.query(
                "SELECT id, origen, entidad_publica_id, estado FROM elementos_revision WHERE familia_id=? AND id_publico=?",
                (rs, fila) -> new RevisionInterna(rs.getLong(1), rs.getString(2), rs.getObject(3, UUID.class), rs.getString(4)),
                familia.getId(), elementoId);
        if (elementos.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Elemento de revisión no encontrado");
        RevisionInterna elemento = elementos.getFirst();
        if (elemento.estado().equals("RESUELTO")) return;
        if (elemento.origen().equals("OCURRENCIA")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La toma debe confirmarse, omitirse, posponerse o cancelarse");
        }
        UUID actor = actor(jwt);
        int cerrados = jdbc.update("UPDATE elementos_revision SET estado='RESUELTO', resuelto_por=?, resuelto_en=NOW(), actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id=? AND estado='PENDIENTE'",
                actor, familia.getId(), elemento.id());
        if (cerrados == 0) return;
        if (elemento.origen().equals("TRATAMIENTO")) {
            jdbc.update("UPDATE tratamientos SET estado='CERRADO', actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id_publico=?",
                    familia.getId(), elemento.entidadId());
        } else if (elemento.origen().equals("LOTE_MEDICAMENTO")) {
            jdbc.update("UPDATE lotes_medicamento SET estado='DESCARTADO', actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id_publico=?",
                    familia.getId(), elemento.entidadId());
        }
        jdbc.update("INSERT INTO acciones_revision (familia_id, elemento_revision_id, clave_idempotencia, accion, actor_publico_id) VALUES (?, ?, ?, 'CERRAR', ?)",
                familia.getId(), elemento.id(), clave, actor);
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, 'CERRAR', ?, ?, 'Elemento cerrado desde Revisar')",
                familia.getId(), actor, elemento.origen(), elemento.entidadId());
    }

    private void materializar(Familia familia) {
        materializar(familia, null);
    }

    private void materializar(Familia familia, Long tratamientoId) {
        String sql = "SELECT t.id tratamiento_id, t.fecha_inicio, t.fecha_fin, h.id horario_id, h.hora_local, h.intervalo_horas "
                + "FROM tratamientos t JOIN horarios_tratamiento h ON h.tratamiento_id=t.id AND h.activo "
                + "WHERE t.familia_id=? AND t.estado='ACTIVO'" + (tratamientoId == null ? "" : " AND t.id=?");
        Object[] parametros = tratamientoId == null ? new Object[] { familia.getId() } : new Object[] { familia.getId(), tratamientoId };
        List<Horario> horarios = jdbc.query(sql, (rs, fila) -> new Horario(rs.getLong("tratamiento_id"),
                rs.getObject("fecha_inicio", LocalDate.class), rs.getObject("fecha_fin", LocalDate.class),
                rs.getLong("horario_id"), rs.getObject("hora_local", LocalTime.class),
                rs.getObject("intervalo_horas", Integer.class)), parametros);
        ZoneId zona = ZoneId.of(familia.getZonaHoraria());
        LocalDate hoy = LocalDate.now(zona);
        for (Horario horario : horarios) {
            LocalDate desde = horario.inicio().isAfter(hoy.minusDays(30)) ? horario.inicio() : hoy.minusDays(30);
            LocalDate limite = hoy.plusDays(7);
            LocalDate hasta = horario.fin() != null && horario.fin().isBefore(limite) ? horario.fin() : limite;
            if (desde.isAfter(hasta)) continue;
            if (horario.intervaloHoras() != null) {
                Instant base = horario.inicio().atTime(horario.hora()).atZone(zona).toInstant();
                Instant ventana = desde.atStartOfDay(zona).toInstant();
                long intervaloSegundos = horario.intervaloHoras() * 3600L;
                long diferencia = Math.max(0, Duration.between(base, ventana).getSeconds());
                long saltos = (diferencia + intervaloSegundos - 1) / intervaloSegundos;
                Instant finExclusivo = hasta.plusDays(1).atStartOfDay(zona).toInstant();
                for (Instant instante = base.plusSeconds(saltos * intervaloSegundos);
                        instante.isBefore(finExclusivo); instante = instante.plusSeconds(intervaloSegundos)) {
                    jdbc.update("INSERT INTO ocurrencias_tratamiento (id_publico, familia_id, tratamiento_id, horario_id, programada_en) VALUES (?, ?, ?, ?, ?) ON CONFLICT (familia_id, tratamiento_id, programada_en) DO NOTHING",
                            UuidV7.nuevo(), familia.getId(), horario.tratamientoId(), horario.horarioId(), Timestamp.from(instante));
                }
                continue;
            }
            for (LocalDate fecha = desde; !fecha.isAfter(hasta); fecha = fecha.plusDays(1)) {
                Instant instante = fecha.atTime(horario.hora()).atZone(zona).toInstant();
                jdbc.update("INSERT INTO ocurrencias_tratamiento (id_publico, familia_id, tratamiento_id, horario_id, programada_en) VALUES (?, ?, ?, ?, ?) ON CONFLICT (familia_id, tratamiento_id, programada_en) DO NOTHING",
                        UuidV7.nuevo(), familia.getId(), horario.tratamientoId(), horario.horarioId(), Timestamp.from(instante));
            }
        }
    }

    private void sincronizarRevision(Long familiaId) {
        jdbc.update("INSERT INTO elementos_revision (id_publico, familia_id, origen, entidad_publica_id, motivo) "
                + "SELECT uuidv7(), familia_id, 'OCURRENCIA', id_publico, 'TOMA_SIN_CONFIRMAR' FROM ocurrencias_tratamiento "
                + "WHERE familia_id=? AND estado='PENDIENTE' AND programada_en < NOW() "
                + "ON CONFLICT (familia_id, origen, entidad_publica_id, motivo) DO NOTHING", familiaId);
        jdbc.update("INSERT INTO elementos_revision (id_publico, familia_id, origen, entidad_publica_id, motivo) "
                + "SELECT uuidv7(), familia_id, 'TRATAMIENTO', id_publico, 'TRATAMIENTO_FINALIZADO' FROM tratamientos "
                + "WHERE familia_id=? AND estado='ACTIVO' AND fecha_fin < CURRENT_DATE "
                + "ON CONFLICT (familia_id, origen, entidad_publica_id, motivo) DO NOTHING", familiaId);
        jdbc.update("INSERT INTO elementos_revision (id_publico, familia_id, origen, entidad_publica_id, motivo) "
                + "SELECT uuidv7(), familia_id, 'LOTE_MEDICAMENTO', id_publico, 'MEDICAMENTO_VENCIDO' FROM lotes_medicamento "
                + "WHERE familia_id=? AND estado NOT IN ('DESCARTADO', 'AGOTADO') AND fecha_vencimiento < CURRENT_DATE "
                + "ON CONFLICT (familia_id, origen, entidad_publica_id, motivo) DO NOTHING", familiaId);
    }

    private List<RespuestaOcurrencias.OcurrenciaResumen> consultarOcurrencias(Long familiaId) {
        return jdbc.query("SELECT o.id_publico, t.id_publico tratamiento_publico, p.id_publico perfil_publico, p.nombre_visible, t.nombre_libre, o.programada_en, o.estado, o.pospuesta_a, o.resuelta_por, COALESCE(actor.nombre_visible, 'Adulto autorizado') resuelta_por_nombre, o.resuelta_en "
                + "FROM ocurrencias_tratamiento o JOIN tratamientos t ON t.id=o.tratamiento_id JOIN perfiles p ON p.id=t.perfil_id "
                + "LEFT JOIN miembros_familia mf ON mf.familia_id=o.familia_id AND mf.usuario_publico_id=o.resuelta_por LEFT JOIN perfiles actor ON actor.id=mf.perfil_id "
                + "WHERE o.familia_id=? AND o.programada_en BETWEEN NOW() - INTERVAL '30 days' AND NOW() + INTERVAL '7 days' ORDER BY o.programada_en",
                (rs, fila) -> mapearOcurrencia(rs), familiaId);
    }

    private List<RespuestaOcurrencias.ElementoRevision> consultarRevision(Long familiaId) {
        return jdbc.query("SELECT r.id_publico, r.origen, r.entidad_publica_id, r.motivo, "
                + "CASE r.origen WHEN 'OCURRENCIA' THEN t.nombre_libre WHEN 'TRATAMIENTO' THEN tf.nombre_libre ELSE m.nombre END titulo, "
                + "CASE r.origen WHEN 'OCURRENCIA' THEN o.programada_en WHEN 'TRATAMIENTO' THEN tf.fecha_fin::timestamptz ELSE l.fecha_vencimiento::timestamptz END fecha, r.estado "
                + "FROM elementos_revision r "
                + "LEFT JOIN ocurrencias_tratamiento o ON r.origen='OCURRENCIA' AND o.id_publico=r.entidad_publica_id "
                + "LEFT JOIN tratamientos t ON t.id=o.tratamiento_id "
                + "LEFT JOIN tratamientos tf ON r.origen='TRATAMIENTO' AND tf.id_publico=r.entidad_publica_id "
                + "LEFT JOIN lotes_medicamento l ON r.origen='LOTE_MEDICAMENTO' AND l.id_publico=r.entidad_publica_id "
                + "LEFT JOIN medicamentos m ON m.id=l.medicamento_id "
                + "WHERE r.familia_id=? AND r.estado='PENDIENTE' ORDER BY fecha NULLS LAST",
                (rs, fila) -> new RespuestaOcurrencias.ElementoRevision(rs.getObject("id_publico", UUID.class),
                        rs.getString("origen"), rs.getObject("entidad_publica_id", UUID.class), rs.getString("motivo"),
                        rs.getString("titulo"), rs.getTimestamp("fecha") == null ? null : rs.getTimestamp("fecha").toInstant(),
                        rs.getString("estado")), familiaId);
    }

    private RespuestaOcurrencias.OcurrenciaResumen buscarOcurrencia(Long familiaId, UUID id) {
        List<RespuestaOcurrencias.OcurrenciaResumen> resultado = jdbc.query("SELECT o.id_publico, t.id_publico tratamiento_publico, p.id_publico perfil_publico, p.nombre_visible, t.nombre_libre, o.programada_en, o.estado, o.pospuesta_a, o.resuelta_por, COALESCE(actor.nombre_visible, 'Adulto autorizado') resuelta_por_nombre, o.resuelta_en "
                + "FROM ocurrencias_tratamiento o JOIN tratamientos t ON t.id=o.tratamiento_id JOIN perfiles p ON p.id=t.perfil_id "
                + "LEFT JOIN miembros_familia mf ON mf.familia_id=o.familia_id AND mf.usuario_publico_id=o.resuelta_por LEFT JOIN perfiles actor ON actor.id=mf.perfil_id "
                + "WHERE o.familia_id=? AND o.id_publico=?",
                (rs, fila) -> mapearOcurrencia(rs), familiaId, id);
        if (resultado.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocurrencia no encontrada");
        return resultado.getFirst();
    }

    private RespuestaOcurrencias.OcurrenciaResumen mapearOcurrencia(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new RespuestaOcurrencias.OcurrenciaResumen(rs.getObject("id_publico", UUID.class),
                rs.getObject("tratamiento_publico", UUID.class), rs.getObject("perfil_publico", UUID.class),
                rs.getString("nombre_visible"), rs.getString("nombre_libre"), rs.getTimestamp("programada_en").toInstant(),
                rs.getString("estado"), rs.getTimestamp("pospuesta_a") == null ? null : rs.getTimestamp("pospuesta_a").toInstant(),
                rs.getObject("resuelta_por", UUID.class), rs.getString("resuelta_por_nombre"),
                rs.getTimestamp("resuelta_en") == null ? null : rs.getTimestamp("resuelta_en").toInstant());
    }

    private OcurrenciaInterna buscarInterna(Long familiaId, UUID id) {
        List<OcurrenciaInterna> resultado = jdbc.query("SELECT id, tratamiento_id, estado FROM ocurrencias_tratamiento WHERE familia_id=? AND id_publico=?",
                (rs, fila) -> new OcurrenciaInterna(rs.getLong(1), rs.getLong(2), rs.getString(3)), familiaId, id);
        if (resultado.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ocurrencia no encontrada");
        return resultado.getFirst();
    }

    private UUID actor(Jwt jwt) {
        try { return UUID.fromString(jwt.getSubject()); }
        catch (RuntimeException error) { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identidad inválida"); }
    }

    private String validarClave(String claveIdempotencia) {
        String clave = claveIdempotencia == null ? "" : claveIdempotencia.trim();
        if (clave.isEmpty() || clave.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key es obligatorio y admite hasta 120 caracteres");
        }
        return clave;
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private record Horario(Long tratamientoId, LocalDate inicio, LocalDate fin, Long horarioId, LocalTime hora,
            Integer intervaloHoras) { }
    private record OcurrenciaInterna(Long id, Long tratamientoId, String estado) { }
    private record AccionExistente(UUID ocurrenciaId, String accion) { }
    private record AccionTratamientoExistente(UUID tratamientoId, String accion) { }
    private record TratamientoInterno(Long id, String estado) { }
    private record RevisionInterna(Long id, String origen, UUID entidadId, String estado) { }
}
