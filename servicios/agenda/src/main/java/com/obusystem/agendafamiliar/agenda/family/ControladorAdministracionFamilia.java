package com.obusystem.agendafamiliar.agenda.family;

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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/familias/{familiaId}/configuracion")
public class ControladorAdministracionFamilia {
    private final ServicioAdministracionFamilia servicio;

    public ControladorAdministracionFamilia(ServicioAdministracionFamilia servicio) { this.servicio = servicio; }

    @GetMapping
    RespuestaFamilia consultar(@PathVariable UUID familiaId, @AuthenticationPrincipal Jwt jwt) {
        return servicio.consultar(familiaId, jwt);
    }

    @PostMapping("/perfiles")
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaFamilia.PerfilAdministrado crear(@PathVariable UUID familiaId,
            @Valid @RequestBody SolicitudPerfil solicitud, @AuthenticationPrincipal Jwt jwt) {
        return servicio.crear(familiaId, solicitud, jwt);
    }

    @PatchMapping("/perfiles/{perfilId}")
    RespuestaFamilia.PerfilAdministrado actualizar(@PathVariable UUID familiaId, @PathVariable UUID perfilId,
            @Valid @RequestBody SolicitudPerfil solicitud, @AuthenticationPrincipal Jwt jwt) {
        return servicio.actualizar(familiaId, perfilId, solicitud, jwt);
    }
}
