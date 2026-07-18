package com.obusystem.agendafamiliar.agenda.hoy;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obusystem.agendafamiliar.agenda.tarea.EstadoTarea;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/familias/{familiaId}")
public class ControladorHoy {
    private final ServicioHoy hoy;

    public ControladorHoy(ServicioHoy hoy) {
        this.hoy = hoy;
    }

    @GetMapping("/hoy")
    RespuestaHoy consultar(@PathVariable UUID familiaId, @AuthenticationPrincipal Jwt jwt) {
        return hoy.consultar(familiaId, jwt);
    }

    @PostMapping("/tareas")
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaHoy.TareaResumen crear(@PathVariable UUID familiaId, @Valid @RequestBody SolicitudTarea solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        return hoy.crearTarea(familiaId, solicitud, jwt);
    }

    @PatchMapping("/tareas/{tareaId}/estado/{estado}")
    RespuestaHoy.TareaResumen cambiarEstado(@PathVariable UUID familiaId, @PathVariable UUID tareaId,
            @PathVariable EstadoTarea estado, @AuthenticationPrincipal Jwt jwt) {
        return hoy.cambiarEstado(familiaId, tareaId, estado, jwt);
    }
}
