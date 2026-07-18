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

@Service
public class ServicioHoy {
    private final AccesoFamilia acceso;
    private final RepositorioPerfiles perfiles;
    private final RepositorioTareas tareas;
    private final JdbcTemplate jdbc;

    public ServicioHoy(AccesoFamilia acceso, RepositorioPerfiles perfiles, RepositorioTareas tareas,
            JdbcTemplate jdbc) {
        this.acceso = acceso;
        this.perfiles = perfiles;
        this.tareas = tareas;
        this.jdbc = jdbc;
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
        Tarea tarea = tareas.save(new Tarea(UuidV7.nuevo(), familia.getId(), perfil.getId(), solicitud.titulo().trim(),
                solicitud.descripcion(), solicitud.fechaLimite()));
        auditar(familia.getId(), jwt, "CREAR", tarea.getIdPublico(), "Tarea familiar registrada");
        return resumenTarea(tarea, perfil);
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
                perfilesFamilia.stream().map(perfil -> new RespuestaHoy.PerfilResumen(perfil.getIdPublico(),
                        perfil.getNombreVisible(), perfil.getTipo(), perfil.getColor(), perfil.getRelacion())).toList(),
                tareasFamilia.stream().map(tarea -> resumenTarea(tarea, perfilesPorId.get(tarea.getPerfilId()))).toList());
    }

    private RespuestaHoy.TareaResumen resumenTarea(Tarea tarea, Perfil perfil) {
        return new RespuestaHoy.TareaResumen(tarea.getIdPublico(), tarea.getTitulo(), tarea.getDescripcion(),
                tarea.getFechaLimite(), tarea.getEstado().name(), perfil.getIdPublico(), perfil.getNombreVisible());
    }

    private void auditar(Long familiaId, Jwt jwt, String operacion, UUID entidadId, String resumen) {
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, ?, 'TAREA', ?, ?)",
                familiaId, UUID.fromString(jwt.getSubject()), operacion, entidadId, resumen);
    }
}
