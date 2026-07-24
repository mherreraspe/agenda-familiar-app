package com.obusystem.agendafamiliar.agenda.notificacion;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/familias/{familiaId}/notificaciones")
public class ControladorNotificaciones {
    private final ServicioNotificaciones notificaciones;

    public ControladorNotificaciones(ServicioNotificaciones notificaciones) {
        this.notificaciones = notificaciones;
    }

    @GetMapping
    RespuestaNotificaciones consultar(@PathVariable UUID familiaId, @AuthenticationPrincipal Jwt jwt) {
        return notificaciones.consultar(familiaId, jwt);
    }

    @PatchMapping("/{notificacionId}/leida")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void marcarLeida(@PathVariable UUID familiaId, @PathVariable UUID notificacionId,
            @AuthenticationPrincipal Jwt jwt) {
        notificaciones.marcarLeida(familiaId, notificacionId, jwt);
    }

    @PostMapping("/leer-todas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void marcarTodasLeidas(@PathVariable UUID familiaId, @AuthenticationPrincipal Jwt jwt) {
        notificaciones.marcarTodasLeidas(familiaId, jwt);
    }

    @PutMapping("/preferencias")
    RespuestaNotificaciones.Preferencias guardarPreferencias(@PathVariable UUID familiaId,
            @Valid @RequestBody SolicitudPreferenciasNotificacion solicitud, @AuthenticationPrincipal Jwt jwt) {
        return notificaciones.guardarPreferencias(familiaId, solicitud, jwt);
    }

    @PostMapping("/suscripciones")
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaNotificaciones.Dispositivo registrarDispositivo(@PathVariable UUID familiaId,
            @Valid @RequestBody SolicitudSuscripcionPush solicitud, @AuthenticationPrincipal Jwt jwt) {
        return notificaciones.registrarDispositivo(familiaId, solicitud, jwt);
    }

    @DeleteMapping("/suscripciones/{dispositivoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revocarDispositivo(@PathVariable UUID familiaId, @PathVariable UUID dispositivoId,
            @AuthenticationPrincipal Jwt jwt) {
        notificaciones.revocarDispositivo(familiaId, dispositivoId, jwt);
    }
}
