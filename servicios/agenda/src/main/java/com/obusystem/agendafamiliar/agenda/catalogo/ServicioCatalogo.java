package com.obusystem.agendafamiliar.agenda.catalogo;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.obusystem.agendafamiliar.agenda.busqueda.EventoCreado;
import com.obusystem.agendafamiliar.agenda.family.AccesoFamilia;
import com.obusystem.agendafamiliar.agenda.family.Familia;
import com.obusystem.agendafamiliar.agenda.tratamiento.ServicioOcurrencias;
import com.obusystem.agendafamiliar.agenda.util.UuidV7;

@Service
public class ServicioCatalogo {
    private final AccesoFamilia acceso;
    private final JdbcTemplate jdbc;
    private final ServicioOcurrencias ocurrencias;
    private final ApplicationEventPublisher eventosAplicacion;

    public ServicioCatalogo(AccesoFamilia acceso, JdbcTemplate jdbc, ServicioOcurrencias ocurrencias,
            ApplicationEventPublisher eventosAplicacion) {
        this.acceso = acceso;
        this.jdbc = jdbc;
        this.ocurrencias = ocurrencias;
        this.eventosAplicacion = eventosAplicacion;
    }

    @Transactional(readOnly = true)
    public RespuestaCatalogo consultar(UUID familiaId, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        return new RespuestaCatalogo(medicamentos(familia.getId()), tratamientos(familia.getId()),
                eventos(familia.getId()), lugares(familia.getId()));
    }

    @Transactional
    public UUID crearMedicamento(UUID familiaId, SolicitudesCatalogo.Medicamento solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        UUID medicamentoId = UuidV7.nuevo();
        UUID loteId = UuidV7.nuevo();
        jdbc.update("INSERT INTO medicamentos (id_publico, familia_id, nombre, presentacion, concentracion) VALUES (?, ?, ?, ?, ?)",
                medicamentoId, familia.getId(), solicitud.nombre().trim(), limpiar(solicitud.presentacion()),
                limpiar(solicitud.concentracion()));
        Long interno = jdbc.queryForObject("SELECT id FROM medicamentos WHERE familia_id = ? AND id_publico = ?", Long.class,
                familia.getId(), medicamentoId);
        jdbc.update("INSERT INTO lotes_medicamento (id_publico, familia_id, medicamento_id, cantidad, unidad, fecha_vencimiento) VALUES (?, ?, ?, ?, ?, ?)",
                loteId, familia.getId(), interno, solicitud.cantidad(), solicitud.unidad(),
                solicitud.fechaVencimiento() == null ? null : Date.valueOf(solicitud.fechaVencimiento()));
        auditar(familia.getId(), jwt, "CREAR", "MEDICAMENTO", medicamentoId, "Medicamento registrado");
        return medicamentoId;
    }

    @Transactional
    public UUID crearTratamiento(UUID familiaId, SolicitudesCatalogo.Tratamiento solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        LocalDate inicio = solicitud.fechaInicio() == null
                ? LocalDate.now(ZoneId.of(familia.getZonaHoraria())) : solicitud.fechaInicio();
        validarRango(inicio, solicitud.fechaFin());
        Long perfil = idInterno("perfiles", familia.getId(), solicitud.perfilId(), "Perfil inválido");
        Long responsable = solicitud.responsablePerfilId() == null ? perfil
                : idInterno("perfiles", familia.getId(), solicitud.responsablePerfilId(), "Responsable inválido");
        Long medicamento = solicitud.medicamentoId() == null ? null
                : idInterno("medicamentos", familia.getId(), solicitud.medicamentoId(), "Medicamento inválido");
        UUID id = UuidV7.nuevo();
        jdbc.update("INSERT INTO tratamientos (id_publico, familia_id, perfil_id, medicamento_id, nombre_libre, responsable_perfil_id, indicacion, dosis_indicada, cantidad_receta, frecuencia, fecha_inicio, fecha_fin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, familia.getId(), perfil, medicamento, solicitud.nombre().trim(), responsable,
                limpiar(solicitud.indicacion()), limpiar(solicitud.cantidadReceta()), limpiar(solicitud.cantidadReceta()),
                limpiar(solicitud.frecuencia()), Date.valueOf(inicio),
                solicitud.fechaFin() == null ? null : Date.valueOf(solicitud.fechaFin()));
        Long tratamientoInterno = jdbc.queryForObject(
                "SELECT id FROM tratamientos WHERE familia_id=? AND id_publico=?", Long.class, familia.getId(), id);
        jdbc.update("INSERT INTO horarios_tratamiento (id_publico, familia_id, tratamiento_id, hora_local) VALUES (?, ?, ?, ?)",
                UuidV7.nuevo(), familia.getId(), tratamientoInterno, Time.valueOf(solicitud.horario()));
        ocurrencias.materializarTratamiento(familia, tratamientoInterno);
        auditar(familia.getId(), jwt, "CREAR", "TRATAMIENTO", id,
                "Tratamiento registrado sin interpretar la indicación");
        return id;
    }

    @Transactional
    public UUID crearEvento(UUID familiaId, SolicitudesCatalogo.Evento solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        if (solicitud.finEn() != null && solicitud.finEn().isBefore(solicitud.inicioEn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior al inicio");
        }
        Long perfil = solicitud.perfilId() == null ? null
                : idInterno("perfiles", familia.getId(), solicitud.perfilId(), "Perfil inválido");
        String lugar = limpiar(solicitud.lugar());
        String direccion = limpiar(solicitud.direccion());
        Long lugarGuardado = guardarLugar(familia.getId(), lugar, direccion);
        UUID id = UuidV7.nuevo();
        jdbc.update("INSERT INTO eventos (id_publico, familia_id, perfil_id, titulo, tipo, lugar, direccion, notas, inicio_en, fin_en, lugar_guardado_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, familia.getId(), perfil, solicitud.titulo().trim(), limpiar(solicitud.tipo()), lugar, direccion,
                limpiar(solicitud.notas()), Timestamp.from(solicitud.inicioEn()),
                solicitud.finEn() == null ? null : Timestamp.from(solicitud.finEn()), lugarGuardado);
        auditar(familia.getId(), jwt, "CREAR", "EVENTO", id, "Evento familiar registrado");
        eventosAplicacion.publishEvent(new EventoCreado(familia.getId(), id));
        return id;
    }

    private List<RespuestaCatalogo.MedicamentoResumen> medicamentos(Long familiaId) {
        return jdbc.query("SELECT m.id_publico, m.nombre, m.presentacion, m.concentracion, l.cantidad, l.unidad, l.fecha_vencimiento, l.estado FROM medicamentos m LEFT JOIN lotes_medicamento l ON l.medicamento_id=m.id WHERE m.familia_id=? ORDER BY m.nombre, l.fecha_vencimiento",
                (rs, fila) -> new RespuestaCatalogo.MedicamentoResumen(rs.getObject("id_publico", UUID.class),
                        rs.getString("nombre"), rs.getString("presentacion"), rs.getString("concentracion"),
                        rs.getBigDecimal("cantidad"), rs.getString("unidad"),
                        rs.getObject("fecha_vencimiento", LocalDate.class), rs.getString("estado")), familiaId);
    }

    private List<RespuestaCatalogo.TratamientoResumen> tratamientos(Long familiaId) {
        return jdbc.query("SELECT t.id_publico, p.id_publico perfil_publico, p.nombre_visible, m.id_publico medicamento_publico, t.nombre_libre medicamento, rp.id_publico responsable_publico, rp.nombre_visible responsable, t.indicacion, COALESCE(t.cantidad_receta, t.dosis_indicada) dosis_indicada, t.frecuencia, t.fecha_inicio, t.fecha_fin, t.estado FROM tratamientos t JOIN perfiles p ON p.id=t.perfil_id JOIN perfiles rp ON rp.id=t.responsable_perfil_id LEFT JOIN medicamentos m ON m.id=t.medicamento_id WHERE t.familia_id=? ORDER BY t.fecha_inicio DESC",
                (rs, fila) -> new RespuestaCatalogo.TratamientoResumen(rs.getObject("id_publico", UUID.class),
                        rs.getObject("perfil_publico", UUID.class), rs.getString("nombre_visible"),
                        rs.getObject("medicamento_publico", UUID.class), rs.getString("medicamento"),
                        rs.getObject("responsable_publico", UUID.class), rs.getString("responsable"),
                        rs.getString("indicacion"), rs.getString("dosis_indicada"), rs.getString("frecuencia"),
                        rs.getObject("fecha_inicio", LocalDate.class), rs.getObject("fecha_fin", LocalDate.class),
                        rs.getString("estado")), familiaId);
    }

    private List<RespuestaCatalogo.EventoResumen> eventos(Long familiaId) {
        return jdbc.query("SELECT e.id_publico, p.id_publico perfil_publico, p.nombre_visible, e.titulo, e.tipo, e.lugar, e.direccion, e.notas, e.inicio_en, e.fin_en, e.estado FROM eventos e LEFT JOIN perfiles p ON p.id=e.perfil_id WHERE e.familia_id=? AND e.inicio_en >= NOW() - INTERVAL '30 days' ORDER BY e.inicio_en",
                (rs, fila) -> new RespuestaCatalogo.EventoResumen(rs.getObject("id_publico", UUID.class),
                        rs.getObject("perfil_publico", UUID.class), rs.getString("nombre_visible"), rs.getString("titulo"),
                        rs.getString("tipo"), rs.getString("lugar"), rs.getString("direccion"), rs.getString("notas"),
                        rs.getTimestamp("inicio_en").toInstant(),
                        rs.getTimestamp("fin_en") == null ? null : rs.getTimestamp("fin_en").toInstant(),
                        rs.getString("estado")), familiaId);
    }

    private List<RespuestaCatalogo.LugarResumen> lugares(Long familiaId) {
        return jdbc.query("SELECT id_publico, nombre, direccion, ultima_utilizacion, frecuencia_uso FROM lugares_familia WHERE familia_id=? ORDER BY ultima_utilizacion DESC, frecuencia_uso DESC, nombre LIMIT 50",
                (rs, fila) -> new RespuestaCatalogo.LugarResumen(rs.getObject("id_publico", UUID.class),
                        rs.getString("nombre"), rs.getString("direccion"),
                        rs.getTimestamp("ultima_utilizacion").toInstant(), rs.getInt("frecuencia_uso")), familiaId);
    }

    private Long guardarLugar(Long familiaId, String nombre, String direccion) {
        if (nombre == null) return null;
        return jdbc.queryForObject("INSERT INTO lugares_familia (id_publico, familia_id, nombre, nombre_normalizado, direccion) VALUES (?, ?, ?, ?, ?) "
                + "ON CONFLICT (familia_id, nombre_normalizado, direccion) DO UPDATE SET nombre=EXCLUDED.nombre, ultima_utilizacion=NOW(), frecuencia_uso=lugares_familia.frecuencia_uso+1, actualizado_en=NOW(), version=lugares_familia.version+1 RETURNING id",
                Long.class, UuidV7.nuevo(), familiaId, nombre, nombre.toLowerCase(Locale.ROOT), direccion);
    }

    private Long idInterno(String tabla, Long familiaId, UUID idPublico, String mensaje) {
        if (!List.of("perfiles", "medicamentos").contains(tabla)) throw new IllegalArgumentException("Tabla no permitida");
        List<Long> ids = jdbc.query("SELECT id FROM " + tabla + " WHERE familia_id = ? AND id_publico = ?",
                (rs, fila) -> rs.getLong(1), familiaId, idPublico);
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
        return ids.getFirst();
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (fin != null && fin.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior al inicio");
        }
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void auditar(Long familiaId, Jwt jwt, String operacion, String entidad, UUID entidadId, String resumen) {
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, ?, ?, ?, ?)",
                familiaId, UUID.fromString(jwt.getSubject()), operacion, entidad, entidadId, resumen);
    }
}
