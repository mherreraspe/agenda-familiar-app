package com.obusystem.agendafamiliar.agenda.hoy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.obusystem.agendafamiliar.agenda.family.AccesoFamilia;
import com.obusystem.agendafamiliar.agenda.family.Familia;
import com.obusystem.agendafamiliar.agenda.family.Perfil;
import com.obusystem.agendafamiliar.agenda.family.RepositorioPerfiles;
import com.obusystem.agendafamiliar.agenda.tarea.EstadoTarea;
import com.obusystem.agendafamiliar.agenda.tarea.RepositorioTareas;
import com.obusystem.agendafamiliar.agenda.tarea.Tarea;
import com.obusystem.agendafamiliar.agenda.util.UuidV7;
import com.obusystem.agendafamiliar.agenda.recurrencia.ServicioRecurrencias;

@Service
public class ServicioHoy {
    private final AccesoFamilia acceso;
    private final RepositorioPerfiles perfiles;
    private final RepositorioTareas tareas;
    private final JdbcTemplate jdbc;
    private final ServicioRecurrencias recurrencias;

    public ServicioHoy(AccesoFamilia acceso, RepositorioPerfiles perfiles, RepositorioTareas tareas,
            JdbcTemplate jdbc, ServicioRecurrencias recurrencias) {
        this.acceso = acceso;
        this.perfiles = perfiles;
        this.tareas = tareas;
        this.jdbc = jdbc;
        this.recurrencias = recurrencias;
    }

    @Transactional(readOnly = true)
    public RespuestaHoy consultar(UUID familiaPublicaId, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaPublicaId, jwt);
        List<Perfil> perfilesFamilia = perfiles.findByFamiliaIdOrderByNombreVisible(familia.getId());
        Instant ahora = Instant.now();
        List<Tarea> proximas = tareas.findByFamiliaIdAndFechaLimiteBetweenOrderByFechaLimite(
                familia.getId(), ahora.minus(30, ChronoUnit.DAYS), ahora.plus(7, ChronoUnit.DAYS));
        return respuesta(familia, perfilesFamilia, proximas);
    }

    @Transactional
    public RespuestaHoy.TareaResumen crearTarea(UUID familiaPublicaId, SolicitudTarea solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaPublicaId, jwt);
        Perfil perfil = perfiles.findByFamiliaIdAndIdPublico(familia.getId(), solicitud.perfilId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Perfil inválido"));
        ServicioRecurrencias.Serie serie = recurrencias.crear(familia.getId(), "TAREA", solicitud.fechaLimite(),
                familia.getZonaHoraria(), solicitud.recurrencia());
        if (!perfil.isActivo()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El perfil está inactivo");
        Tarea primera = null;
        for (int indice = 0; indice < serie.fechas().size(); indice++) {
            Tarea instancia = tareas.save(new Tarea(UuidV7.nuevo(), familia.getId(), perfil.getId(),
                    solicitud.titulo().trim(), solicitud.descripcion(), serie.fechas().get(indice), serie.id(),
                    serie.id() == null ? null : indice + 1, !Boolean.FALSE.equals(solicitud.avisar())));
            if (primera == null) primera = instancia;
        }
        auditar(familia.getId(), jwt, "CREAR", primera.getIdPublico(), serie.id() == null
                ? "Tarea familiar registrada" : "Serie de tareas registrada");
        return resumenTarea(primera, perfil);
    }

    @Transactional
    public RespuestaHoy.TareaResumen cambiarEstado(UUID familiaPublicaId, UUID tareaId, EstadoTarea estado, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaPublicaId, jwt);
        Tarea tarea = tareas.findByFamiliaIdAndIdPublico(familia.getId(), tareaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tarea no encontrada"));
        tarea.cambiarEstado(estado);
        auditar(familia.getId(), jwt, estado.name(), tarea.getIdPublico(),
                "Tarea marcada como " + estado.name().toLowerCase());
        Perfil perfil = perfiles.findById(tarea.getPerfilId()).orElseThrow();
        return resumenTarea(tarea, perfil);
    }

    private RespuestaHoy respuesta(Familia familia, List<Perfil> perfilesFamilia, List<Tarea> tareasFamilia) {
        Map<Long, Perfil> perfilesPorId = perfilesFamilia.stream()
                .collect(Collectors.toMap(Perfil::getId, Function.identity()));
        return new RespuestaHoy(familia.getIdPublico(), familia.getNombre(), familia.getZonaHoraria(),
                perfilesFamilia.stream().filter(Perfil::isActivo).map(perfil -> new RespuestaHoy.PerfilResumen(perfil.getIdPublico(),
                        perfil.getNombreVisible(), perfil.getTipo(), perfil.getColor(), perfil.getRelacion())).toList(),
                tareasFamilia.stream().map(tarea -> resumenTarea(tarea, perfilesPorId.get(tarea.getPerfilId()))).toList());
    }

    private RespuestaHoy.TareaResumen resumenTarea(Tarea tarea, Perfil perfil) {
        return new RespuestaHoy.TareaResumen(tarea.getIdPublico(), tarea.getTitulo(), tarea.getDescripcion(),
                tarea.getFechaLimite(), tarea.getEstado().name(), perfil.getIdPublico(), perfil.getNombreVisible(),
                tarea.getRecurrenciaId() != null, idPublicoOrigen(tarea.getTareaOrigenId()));
    }

    private UUID idPublicoOrigen(Long origenId) {
        if (origenId == null) return null;
        List<UUID> ids = jdbc.query("SELECT id_publico FROM tareas WHERE id=?", (rs, fila) -> rs.getObject(1, UUID.class), origenId);
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private void auditar(Long familiaId, Jwt jwt, String operacion, UUID entidadId, String resumen) {
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, ?, 'TAREA', ?, ?)",
                familiaId, UUID.fromString(jwt.getSubject()), operacion, entidadId, resumen);
    }
}
