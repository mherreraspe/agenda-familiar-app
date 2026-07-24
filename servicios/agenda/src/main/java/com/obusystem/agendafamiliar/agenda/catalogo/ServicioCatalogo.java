package com.obusystem.agendafamiliar.agenda.catalogo;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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
import com.obusystem.agendafamiliar.agenda.recurrencia.ServicioRecurrencias;

@Service
public class ServicioCatalogo {
    public record ResultadoMedicamento(UUID id, UUID loteId) { }
    public record ResultadoTratamientos(UUID grupoId, List<UUID> ids) { }
    private final AccesoFamilia acceso;
    private final JdbcTemplate jdbc;
    private final ServicioOcurrencias ocurrencias;
    private final ApplicationEventPublisher eventosAplicacion;
    private final ServicioRecurrencias recurrencias;

    public ServicioCatalogo(AccesoFamilia acceso, JdbcTemplate jdbc, ServicioOcurrencias ocurrencias,
            ApplicationEventPublisher eventosAplicacion, ServicioRecurrencias recurrencias) {
        this.acceso = acceso;
        this.jdbc = jdbc;
        this.ocurrencias = ocurrencias;
        this.eventosAplicacion = eventosAplicacion;
        this.recurrencias = recurrencias;
    }

    @Transactional(readOnly = true)
    public RespuestaCatalogo consultar(UUID familiaId, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        return new RespuestaCatalogo(medicamentos(familia.getId()), tratamientos(familia.getId()),
                eventos(familia.getId()), lugares(familia.getId()));
    }

    @Transactional
    public UUID crearMedicamento(UUID familiaId, SolicitudesCatalogo.Medicamento solicitud, Jwt jwt) {
        return crearMedicamento(familiaId, UUID.randomUUID().toString(), solicitud, jwt).id();
    }

    @Transactional
    public ResultadoMedicamento crearMedicamento(UUID familiaId, String clave, SolicitudesCatalogo.Medicamento solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        validarClave(clave);
        bloquear(familia.getId(), clave);
        ResultadoMedicamento anterior = resultadoMedicamento(familia.getId(), clave, "CREAR_MEDICAMENTO");
        if (anterior != null) return anterior;
        validarApertura(solicitud.estadoEnvase(), solicitud.abiertoEn());
        UUID medicamentoId = UuidV7.nuevo();
        UUID loteId = UuidV7.nuevo();
        jdbc.update("INSERT INTO medicamentos (id_publico, familia_id, nombre, presentacion, concentracion) VALUES (?, ?, ?, ?, ?)",
                medicamentoId, familia.getId(), solicitud.nombre().trim(), limpiar(solicitud.presentacion()),
                limpiar(solicitud.concentracion()));
        Long interno = jdbc.queryForObject("SELECT id FROM medicamentos WHERE familia_id = ? AND id_publico = ?", Long.class,
                familia.getId(), medicamentoId);
        jdbc.update("INSERT INTO lotes_medicamento (id_publico, familia_id, medicamento_id, cantidad, unidad, fecha_vencimiento, estado_envase, abierto_en, duracion_abierto_dias, avisar_vencimiento, anticipacion_vencimiento_dias, avisar_apertura, anticipacion_apertura_dias) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                loteId, familia.getId(), interno, solicitud.cantidad(), solicitud.unidad(),
                solicitud.fechaVencimiento() == null ? null : Date.valueOf(solicitud.fechaVencimiento()),
                valor(solicitud.estadoEnvase(), "SIN_ABRIR"),
                solicitud.abiertoEn() == null ? null : Date.valueOf(solicitud.abiertoEn()), solicitud.duracionAbiertoDias(),
                valor(solicitud.avisarVencimiento(), true), valor(solicitud.anticipacionVencimientoDias(), 7),
                valor(solicitud.avisarApertura(), true), valor(solicitud.anticipacionAperturaDias(), 3));
        guardarResultado(familia.getId(), clave, "CREAR_MEDICAMENTO", medicamentoId, loteId.toString());
        auditar(familia.getId(), jwt, "CREAR", "MEDICAMENTO", medicamentoId, "Medicamento registrado");
        return new ResultadoMedicamento(medicamentoId, loteId);
    }

    @Transactional
    public UUID crearTratamiento(UUID familiaId, SolicitudesCatalogo.Tratamiento solicitud, Jwt jwt) {
        return crearTratamiento(familiaId, UUID.randomUUID().toString(), solicitud, jwt);
    }

    @Transactional
    public UUID crearTratamiento(UUID familiaId, String clave, SolicitudesCatalogo.Tratamiento solicitud, Jwt jwt) {
        List<LocalTime> horarios = solicitud.horarios() == null ? new ArrayList<>() : new ArrayList<>(solicitud.horarios());
        if (solicitud.horario() != null) horarios.add(solicitud.horario());
        return crearTratamientos(familiaId, clave, new SolicitudesCatalogo.TratamientoMultiple(
                List.of(solicitud.perfilId()), solicitud.medicamentoId(), solicitud.nombre(), null,
                solicitud.cantidadReceta(), null, solicitud.indicacion(), solicitud.frecuencia(), horarios,
                solicitud.intervaloHoras(), solicitud.fechaInicio(), solicitud.fechaFin(),
                solicitud.responsablePerfilId(), solicitud.responsableAlternativoPerfilId()), jwt).ids().getFirst();
    }

    @Transactional
    public ResultadoTratamientos crearTratamientos(UUID familiaId, String clave,
            SolicitudesCatalogo.TratamientoMultiple solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        validarClave(clave);
        bloquear(familia.getId(), clave);
        ResultadoTratamientos anterior = resultadoTratamientos(familia.getId(), clave, "CREAR_TRATAMIENTOS");
        if (anterior != null) return anterior;
        List<UUID> perfiles = new ArrayList<>(new LinkedHashSet<>(solicitud.perfilIds()));
        if (perfiles.size() != solicitud.perfilIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No repitas una persona en el tratamiento");
        }
        LocalDate inicio = solicitud.fechaInicio() == null
                ? LocalDate.now(ZoneId.of(familia.getZonaHoraria())) : solicitud.fechaInicio();
        validarRango(inicio, solicitud.fechaFin());
        List<LocalTime> horarios = horariosSolicitados(solicitud.horarios(), solicitud.intervaloHoras());
        Long medicamento = solicitud.medicamentoId() == null ? null
                : idInterno("medicamentos", familia.getId(), solicitud.medicamentoId(), "Medicamento inválido");
        UUID grupoId = UuidV7.nuevo();
        List<UUID> ids = new ArrayList<>();
        for (UUID perfilId : perfiles) {
            Long perfil = idInterno("perfiles", familia.getId(), perfilId, "Perfil inválido");
            Long responsable = solicitud.responsablePerfilId() == null ? perfil
                    : idInterno("perfiles", familia.getId(), solicitud.responsablePerfilId(), "Responsable inválido");
            Long alternativo = solicitud.responsableAlternativoPerfilId() == null ? null
                    : idInterno("perfiles", familia.getId(), solicitud.responsableAlternativoPerfilId(), "Responsable alternativo inválido");
            UUID id = UuidV7.nuevo();
            jdbc.update("INSERT INTO tratamientos (id_publico, grupo_publico_id, familia_id, perfil_id, medicamento_id, nombre_libre, nombre_medicamento, aplicacion, responsable_perfil_id, responsable_alternativo_perfil_id, indicacion, dosis_indicada, cantidad_receta, frecuencia, fecha_inicio, fecha_fin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?)",
                    id, grupoId, familia.getId(), perfil, medicamento, solicitud.nombre().trim(),
                    limpiar(solicitud.nombreMedicamento()), limpiar(solicitud.aplicacion()), responsable, alternativo,
                    limpiar(solicitud.indicacion()), limpiar(solicitud.dosis()), limpiar(solicitud.frecuencia()),
                    Date.valueOf(inicio), solicitud.fechaFin() == null ? null : Date.valueOf(solicitud.fechaFin()));
            Long interno = jdbc.queryForObject("SELECT id FROM tratamientos WHERE familia_id=? AND id_publico=?",
                    Long.class, familia.getId(), id);
            for (LocalTime horario : horarios) {
                jdbc.update("INSERT INTO horarios_tratamiento (id_publico, familia_id, tratamiento_id, hora_local, intervalo_horas) VALUES (?, ?, ?, ?, ?)",
                        UuidV7.nuevo(), familia.getId(), interno, Time.valueOf(horario), solicitud.intervaloHoras());
            }
            ocurrencias.materializarTratamiento(familia, interno);
            ids.add(id);
        }
        guardarResultado(familia.getId(), clave, "CREAR_TRATAMIENTOS", grupoId,
                ids.stream().map(UUID::toString).collect(java.util.stream.Collectors.joining(",")));
        auditar(familia.getId(), jwt, "CREAR", "TRATAMIENTO", grupoId,
                "Tratamiento registrado para " + ids.size() + " persona(s)");
        return new ResultadoTratamientos(grupoId, List.copyOf(ids));
    }

    @Transactional
    public void actualizarTratamiento(UUID familiaId, UUID grupoId, String clave,
            SolicitudesCatalogo.ActualizacionTratamiento solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        validarClave(clave);
        bloquear(familia.getId(), clave);
        if (resultadoPrincipal(familia.getId(), clave, "ACTUALIZAR_TRATAMIENTO") != null) return;
        validarRango(solicitud.fechaInicio(), solicitud.fechaFin());
        List<LocalTime> horarios = horariosSolicitados(solicitud.horarios(), solicitud.intervaloHoras());
        Long medicamento = solicitud.medicamentoId() == null ? null
                : idInterno("medicamentos", familia.getId(), solicitud.medicamentoId(), "Medicamento inválido");
        Long responsable = solicitud.responsablePerfilId() == null ? null
                : idInterno("perfiles", familia.getId(), solicitud.responsablePerfilId(), "Responsable inválido");
        Long alternativo = solicitud.responsableAlternativoPerfilId() == null ? null
                : idInterno("perfiles", familia.getId(), solicitud.responsableAlternativoPerfilId(), "Responsable alternativo inválido");
        List<TratamientoEditable> tratamientos = jdbc.query("SELECT id,id_publico,perfil_id FROM tratamientos WHERE familia_id=? AND grupo_publico_id=? AND estado='ACTIVO' FOR UPDATE",
                (rs, fila) -> new TratamientoEditable(rs.getLong("id"), rs.getObject("id_publico", UUID.class),
                        rs.getLong("perfil_id")), familia.getId(), grupoId);
        if (tratamientos.isEmpty()) {
            Boolean existe = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM tratamientos WHERE familia_id=? AND grupo_publico_id=?)",
                    Boolean.class, familia.getId(), grupoId);
            if (Boolean.TRUE.equals(existe)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Un tratamiento cerrado ya no se puede editar");
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tratamiento no encontrado");
        }
        for (TratamientoEditable tratamiento : tratamientos) {
            Long responsableFinal = responsable == null ? tratamiento.perfilId() : responsable;
            jdbc.update("UPDATE tratamientos SET medicamento_id=?, nombre_libre=?, nombre_medicamento=?, aplicacion=?, responsable_perfil_id=?, responsable_alternativo_perfil_id=?, indicacion=?, dosis_indicada=?, frecuencia=?, fecha_inicio=?, fecha_fin=?, actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id=? AND estado='ACTIVO'",
                    medicamento, solicitud.nombre().trim(), limpiar(solicitud.nombreMedicamento()),
                    limpiar(solicitud.aplicacion()), responsableFinal, alternativo, limpiar(solicitud.indicacion()),
                    limpiar(solicitud.dosis()), limpiar(solicitud.frecuencia()), Date.valueOf(solicitud.fechaInicio()),
                    solicitud.fechaFin() == null ? null : Date.valueOf(solicitud.fechaFin()), familia.getId(), tratamiento.id());
            jdbc.update("DELETE FROM elementos_revision WHERE familia_id=? AND origen='OCURRENCIA' AND entidad_publica_id IN (SELECT id_publico FROM ocurrencias_tratamiento WHERE familia_id=? AND tratamiento_id=? AND estado='PENDIENTE' AND programada_en>=NOW())",
                    familia.getId(), familia.getId(), tratamiento.id());
            jdbc.update("DELETE FROM ocurrencias_tratamiento WHERE familia_id=? AND tratamiento_id=? AND estado='PENDIENTE' AND programada_en>=NOW()",
                    familia.getId(), tratamiento.id());
            jdbc.update("UPDATE horarios_tratamiento SET activo=FALSE, actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND tratamiento_id=? AND activo",
                    familia.getId(), tratamiento.id());
            for (LocalTime horario : horarios) {
                jdbc.update("INSERT INTO horarios_tratamiento (id_publico,familia_id,tratamiento_id,hora_local,intervalo_horas) VALUES (?,?,?,?,?) ON CONFLICT (familia_id,tratamiento_id,hora_local) DO UPDATE SET activo=TRUE,intervalo_horas=EXCLUDED.intervalo_horas,actualizado_en=NOW(),version=horarios_tratamiento.version+1",
                        UuidV7.nuevo(), familia.getId(), tratamiento.id(), Time.valueOf(horario), solicitud.intervaloHoras());
            }
            ocurrencias.materializarTratamientoDesdeAhora(familia, tratamiento.id());
        }
        guardarResultado(familia.getId(), clave, "ACTUALIZAR_TRATAMIENTO", grupoId,
                tratamientos.stream().map(item -> item.idPublico().toString()).collect(java.util.stream.Collectors.joining(",")));
        auditar(familia.getId(), jwt, "ACTUALIZAR", "TRATAMIENTO", grupoId,
                "Tratamiento activo actualizado; las tomas resueltas se conservaron");
    }

    @Transactional
    public void actualizarEnvase(UUID familiaId, UUID loteId, String clave,
            SolicitudesCatalogo.ActualizacionEnvase solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        validarClave(clave);
        bloquear(familia.getId(), clave);
        if (resultadoPrincipal(familia.getId(), clave, "ACTUALIZAR_ENVASE") != null) return;
        validarApertura(solicitud.estadoEnvase(), solicitud.abiertoEn());
        List<String> estadoActual = jdbc.query("SELECT estado_envase FROM lotes_medicamento WHERE familia_id=? AND id_publico=?",
                (rs, fila) -> rs.getString(1), familia.getId(), loteId);
        if (estadoActual.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Envase no encontrado");
        if ("ABIERTO".equals(estadoActual.getFirst()) && "SIN_ABRIR".equals(solicitud.estadoEnvase())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un envase abierto no puede volver a marcarse sin abrir");
        }
        int filas = jdbc.update("UPDATE lotes_medicamento SET estado_envase=?, abierto_en=?, duracion_abierto_dias=?, avisar_vencimiento=?, anticipacion_vencimiento_dias=?, avisar_apertura=?, anticipacion_apertura_dias=?, estado=COALESCE(?, estado), actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id_publico=? AND version=?",
                solicitud.estadoEnvase(), solicitud.abiertoEn() == null ? null : Date.valueOf(solicitud.abiertoEn()),
                solicitud.duracionAbiertoDias(), valor(solicitud.avisarVencimiento(), true),
                valor(solicitud.anticipacionVencimientoDias(), 7), valor(solicitud.avisarApertura(), true),
                valor(solicitud.anticipacionAperturaDias(), 3), solicitud.estadoInventario(), familia.getId(), loteId,
                solicitud.version());
        if (filas == 0) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "El envase cambió en otro dispositivo. Recarga antes de editarlo.");
        guardarResultado(familia.getId(), clave, "ACTUALIZAR_ENVASE", loteId, "");
        auditar(familia.getId(), jwt, "ACTUALIZAR", "LOTE_MEDICAMENTO", loteId, "Estado del envase actualizado");
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
        ServicioRecurrencias.Serie serie = recurrencias.crear(familia.getId(), "EVENTO", solicitud.inicioEn(),
                familia.getZonaHoraria(), solicitud.recurrencia());
        java.time.Duration duracion = solicitud.finEn() == null ? null
                : java.time.Duration.between(solicitud.inicioEn(), solicitud.finEn());
        UUID primera = null;
        for (int indice = 0; indice < serie.fechas().size(); indice++) {
            UUID id = UuidV7.nuevo();
            java.time.Instant inicio = serie.fechas().get(indice);
            jdbc.update("INSERT INTO eventos (id_publico, familia_id, perfil_id, titulo, tipo, lugar, direccion, notas, inicio_en, fin_en, lugar_guardado_id, recurrencia_id, numero_ocurrencia, avisar_24h, avisar_1h) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    id, familia.getId(), perfil, solicitud.titulo().trim(), limpiar(solicitud.tipo()), lugar, direccion,
                    limpiar(solicitud.notas()), Timestamp.from(inicio),
                    duracion == null ? null : Timestamp.from(inicio.plus(duracion)), lugarGuardado, serie.id(),
                    serie.id() == null ? null : indice + 1, !Boolean.FALSE.equals(solicitud.avisar24h()),
                    !Boolean.FALSE.equals(solicitud.avisar1h()));
            if (primera == null) primera = id;
            eventosAplicacion.publishEvent(new EventoCreado(familia.getId(), id));
        }
        auditar(familia.getId(), jwt, "CREAR", "EVENTO", primera, serie.id() == null
                ? "Evento familiar registrado" : "Serie de eventos registrada");
        return primera;
    }

    private List<RespuestaCatalogo.MedicamentoResumen> medicamentos(Long familiaId) {
        return jdbc.query("SELECT m.id_publico, l.id_publico lote_publico, m.nombre, m.presentacion, m.concentracion, l.cantidad, l.unidad, l.fecha_vencimiento, l.estado_envase, l.abierto_en, l.duracion_abierto_dias, l.avisar_vencimiento, l.anticipacion_vencimiento_dias, l.avisar_apertura, l.anticipacion_apertura_dias, l.version, "
                + "CASE WHEN l.estado_envase='ABIERTO' AND l.duracion_abierto_dias IS NOT NULL THEN l.abierto_en + l.duracion_abierto_dias::INTEGER END fecha_limite_apertura, "
                + "CASE WHEN l.fecha_vencimiento IS NULL THEN CASE WHEN l.estado_envase='ABIERTO' AND l.duracion_abierto_dias IS NOT NULL THEN l.abierto_en + l.duracion_abierto_dias::INTEGER END WHEN l.estado_envase='ABIERTO' AND l.duracion_abierto_dias IS NOT NULL THEN LEAST(l.fecha_vencimiento, l.abierto_en + l.duracion_abierto_dias::INTEGER) ELSE l.fecha_vencimiento END vigente_hasta, "
                + "CASE WHEN l.estado_envase='ABIERTO' AND l.duracion_abierto_dias IS NOT NULL AND (l.fecha_vencimiento IS NULL OR l.abierto_en + l.duracion_abierto_dias::INTEGER <= l.fecha_vencimiento) THEN 'DESPUES_DE_ABRIR' WHEN l.fecha_vencimiento IS NOT NULL THEN 'VENCIMIENTO_IMPRESO' END motivo_vigencia, "
                + "CASE WHEN l.estado='DESCARTADO' THEN 'DESCARTADO' "
                + "WHEN l.estado='AGOTADO' OR COALESCE(l.cantidad, 0)=0 THEN 'AGOTADO' "
                + "WHEN (CASE WHEN l.fecha_vencimiento IS NULL THEN CASE WHEN l.estado_envase='ABIERTO' AND l.duracion_abierto_dias IS NOT NULL THEN l.abierto_en + l.duracion_abierto_dias::INTEGER END WHEN l.estado_envase='ABIERTO' AND l.duracion_abierto_dias IS NOT NULL THEN LEAST(l.fecha_vencimiento, l.abierto_en + l.duracion_abierto_dias::INTEGER) ELSE l.fecha_vencimiento END) < CURRENT_DATE THEN 'VENCIDO' "
                + "WHEN (l.avisar_vencimiento AND l.fecha_vencimiento BETWEEN CURRENT_DATE AND CURRENT_DATE + l.anticipacion_vencimiento_dias::INTEGER) OR (l.avisar_apertura AND l.estado_envase='ABIERTO' AND l.duracion_abierto_dias IS NOT NULL AND l.abierto_en + l.duracion_abierto_dias::INTEGER BETWEEN CURRENT_DATE AND CURRENT_DATE + l.anticipacion_apertura_dias::INTEGER) THEN 'POR_VENCER' "
                + "ELSE 'DISPONIBLE' END estado_calculado, "
                + "CASE WHEN l.estado NOT IN ('AGOTADO','DESCARTADO') THEN ((l.avisar_vencimiento AND l.fecha_vencimiento <= CURRENT_DATE + l.anticipacion_vencimiento_dias::INTEGER) OR (l.avisar_apertura AND l.estado_envase='ABIERTO' AND l.duracion_abierto_dias IS NOT NULL AND l.abierto_en + l.duracion_abierto_dias::INTEGER <= CURRENT_DATE + l.anticipacion_apertura_dias::INTEGER)) ELSE FALSE END requiere_atencion "
                + "FROM medicamentos m LEFT JOIN lotes_medicamento l ON l.medicamento_id=m.id AND l.familia_id=m.familia_id "
                + "WHERE m.familia_id=? ORDER BY m.nombre, l.fecha_vencimiento",
                (rs, fila) -> new RespuestaCatalogo.MedicamentoResumen(rs.getObject("id_publico", UUID.class),
                        rs.getObject("lote_publico", UUID.class),
                        rs.getString("nombre"), rs.getString("presentacion"), rs.getString("concentracion"),
                        rs.getBigDecimal("cantidad"), rs.getString("unidad"),
                        rs.getObject("fecha_vencimiento", LocalDate.class), rs.getString("estado_envase"),
                        rs.getObject("abierto_en", LocalDate.class), rs.getObject("duracion_abierto_dias", Integer.class),
                        rs.getObject("fecha_limite_apertura", LocalDate.class), rs.getObject("vigente_hasta", LocalDate.class),
                        rs.getString("motivo_vigencia"), rs.getBoolean("avisar_vencimiento"),
                        rs.getInt("anticipacion_vencimiento_dias"), rs.getBoolean("avisar_apertura"),
                        rs.getInt("anticipacion_apertura_dias"), rs.getString("estado_calculado"),
                        rs.getBoolean("requiere_atencion"), rs.getLong("version")), familiaId);
    }

    private List<RespuestaCatalogo.TratamientoResumen> tratamientos(Long familiaId) {
        return jdbc.query("SELECT t.id tratamiento_interno, t.id_publico, t.grupo_publico_id, p.id_publico perfil_publico, p.nombre_visible, m.id_publico medicamento_publico, t.nombre_libre medicamento, t.nombre_medicamento, t.aplicacion, rp.id_publico responsable_publico, rp.nombre_visible responsable, rap.id_publico responsable_alternativo_publico, rap.nombre_visible responsable_alternativo, t.indicacion, t.dosis_indicada, t.frecuencia, t.fecha_inicio, t.fecha_fin, t.estado, receta.id_publico receta_publica FROM tratamientos t JOIN perfiles p ON p.id=t.perfil_id JOIN perfiles rp ON rp.id=t.responsable_perfil_id LEFT JOIN perfiles rap ON rap.id=t.responsable_alternativo_perfil_id LEFT JOIN medicamentos m ON m.id=t.medicamento_id LEFT JOIN LATERAL (SELECT ar.id_publico FROM tratamientos tg JOIN archivos_familia ar ON ar.tratamiento_id=tg.id AND ar.familia_id=tg.familia_id AND ar.estado='ACTIVO' WHERE tg.familia_id=t.familia_id AND tg.grupo_publico_id=t.grupo_publico_id ORDER BY ar.creado_en LIMIT 1) receta ON TRUE WHERE t.familia_id=? ORDER BY t.fecha_inicio DESC",
                (rs, fila) -> new RespuestaCatalogo.TratamientoResumen(rs.getObject("id_publico", UUID.class),
                        rs.getObject("grupo_publico_id", UUID.class), rs.getObject("perfil_publico", UUID.class),
                        rs.getString("nombre_visible"), rs.getObject("medicamento_publico", UUID.class),
                        rs.getString("medicamento"), rs.getString("nombre_medicamento"), rs.getString("aplicacion"),
                        rs.getObject("responsable_publico", UUID.class), rs.getString("responsable"),
                        rs.getObject("responsable_alternativo_publico", UUID.class), rs.getString("responsable_alternativo"),
                        rs.getString("indicacion"), rs.getString("dosis_indicada"), rs.getString("frecuencia"),
                        horarios(rs.getLong("tratamiento_interno")), intervalo(rs.getLong("tratamiento_interno")),
                        rs.getObject("fecha_inicio", LocalDate.class), rs.getObject("fecha_fin", LocalDate.class),
                        rs.getString("estado"), rs.getObject("receta_publica", UUID.class)), familiaId);
    }

    private List<LocalTime> horarios(Long tratamientoId) {
        return jdbc.query("SELECT hora_local FROM horarios_tratamiento WHERE tratamiento_id=? AND activo ORDER BY hora_local",
                (rs, fila) -> rs.getObject(1, LocalTime.class), tratamientoId);
    }

    private Integer intervalo(Long tratamientoId) {
        List<Integer> valores = jdbc.query("SELECT intervalo_horas FROM horarios_tratamiento WHERE tratamiento_id=? AND activo AND intervalo_horas IS NOT NULL LIMIT 1",
                (rs, fila) -> rs.getInt(1), tratamientoId);
        return valores.isEmpty() ? null : valores.getFirst();
    }

    private List<RespuestaCatalogo.EventoResumen> eventos(Long familiaId) {
        return jdbc.query("SELECT e.id_publico, p.id_publico perfil_publico, p.nombre_visible, e.titulo, e.tipo, e.lugar, e.direccion, e.notas, e.inicio_en, e.fin_en, e.estado, e.recurrencia_id IS NOT NULL recurrente, origen.id_publico evento_origen_publico FROM eventos e LEFT JOIN perfiles p ON p.id=e.perfil_id LEFT JOIN eventos origen ON origen.id=e.evento_origen_id WHERE e.familia_id=? AND e.inicio_en >= NOW() - INTERVAL '30 days' ORDER BY e.inicio_en",
                (rs, fila) -> new RespuestaCatalogo.EventoResumen(rs.getObject("id_publico", UUID.class),
                        rs.getObject("perfil_publico", UUID.class), rs.getString("nombre_visible"), rs.getString("titulo"),
                        rs.getString("tipo"), rs.getString("lugar"), rs.getString("direccion"), rs.getString("notas"),
                        rs.getTimestamp("inicio_en").toInstant(),
                        rs.getTimestamp("fin_en") == null ? null : rs.getTimestamp("fin_en").toInstant(),
                        rs.getString("estado"), rs.getBoolean("recurrente"),
                        rs.getObject("evento_origen_publico", UUID.class)), familiaId);
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
        String activo = tabla.equals("perfiles") ? " AND activo" : "";
        List<Long> ids = jdbc.query("SELECT id FROM " + tabla + " WHERE familia_id = ? AND id_publico = ?" + activo,
                (rs, fila) -> rs.getLong(1), familiaId, idPublico);
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
        return ids.getFirst();
    }

    private void validarRango(LocalDate inicio, LocalDate fin) {
        if (fin != null && fin.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha final no puede ser anterior al inicio");
        }
    }

    private List<LocalTime> horariosSolicitados(List<LocalTime> solicitados, Integer intervaloHoras) {
        List<LocalTime> horarios = solicitados.stream().distinct().sorted().toList();
        if (horarios.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar al menos un horario");
        if (intervaloHoras != null && horarios.size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un intervalo requiere un único horario inicial");
        }
        return horarios;
    }

    private void validarApertura(String estado, LocalDate abiertoEn) {
        String normalizado = valor(estado, "SIN_ABRIR");
        if ("ABIERTO".equals(normalizado) && abiertoEn == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica cuándo se abrió el envase");
        }
        if (abiertoEn != null && abiertoEn.isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de apertura no puede estar en el futuro");
        }
    }

    private void validarClave(String clave) {
        if (clave == null || clave.isBlank() || clave.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key inválida");
        }
    }

    private void bloquear(Long familiaId, String clave) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", (rs, fila) -> 0,
                familiaId + ":salud:" + clave);
    }

    private UUID resultadoPrincipal(Long familiaId, String clave, String operacion) {
        List<String[]> filas = jdbc.query("SELECT operacion, resultado_principal::TEXT FROM idempotencia_salud WHERE familia_id=? AND clave=?",
                (rs, fila) -> new String[] { rs.getString(1), rs.getString(2) }, familiaId, clave);
        if (filas.isEmpty()) return null;
        if (!operacion.equals(filas.getFirst()[0])) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave de idempotencia ya se usó en otra operación");
        }
        return UUID.fromString(filas.getFirst()[1]);
    }

    private ResultadoMedicamento resultadoMedicamento(Long familiaId, String clave, String operacion) {
        UUID principal = resultadoPrincipal(familiaId, clave, operacion);
        if (principal == null) return null;
        String ids = jdbc.queryForObject("SELECT resultado_ids FROM idempotencia_salud WHERE familia_id=? AND clave=?",
                String.class, familiaId, clave);
        return new ResultadoMedicamento(principal, UUID.fromString(ids));
    }

    private ResultadoTratamientos resultadoTratamientos(Long familiaId, String clave, String operacion) {
        UUID principal = resultadoPrincipal(familiaId, clave, operacion);
        if (principal == null) return null;
        String ids = jdbc.queryForObject("SELECT resultado_ids FROM idempotencia_salud WHERE familia_id=? AND clave=?",
                String.class, familiaId, clave);
        List<UUID> resultados = ids == null || ids.isBlank() ? List.of()
                : java.util.Arrays.stream(ids.split(",")).map(UUID::fromString).toList();
        return new ResultadoTratamientos(principal, resultados);
    }

    private void guardarResultado(Long familiaId, String clave, String operacion, UUID principal, String ids) {
        jdbc.update("INSERT INTO idempotencia_salud (familia_id, clave, operacion, resultado_principal, resultado_ids) VALUES (?, ?, ?, ?, ?)",
                familiaId, clave, operacion, principal, ids);
    }

    private <T> T valor(T recibido, T porDefecto) {
        return recibido == null ? porDefecto : recibido;
    }

    private record TratamientoEditable(Long id, UUID idPublico, Long perfilId) { }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void auditar(Long familiaId, Jwt jwt, String operacion, String entidad, UUID entidadId, String resumen) {
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, ?, ?, ?, ?)",
                familiaId, UUID.fromString(jwt.getSubject()), operacion, entidad, entidadId, resumen);
    }
}
