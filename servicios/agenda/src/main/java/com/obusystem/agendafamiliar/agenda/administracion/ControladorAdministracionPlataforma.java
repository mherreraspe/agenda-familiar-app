package com.obusystem.agendafamiliar.agenda.administracion;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/administracion/familias")
public class ControladorAdministracionPlataforma {
    private final ServicioAdministracionPlataforma administracion;

    public ControladorAdministracionPlataforma(ServicioAdministracionPlataforma administracion) {
        this.administracion = administracion;
    }

    @GetMapping
    RespuestaFamiliasPlataforma consultar(@AuthenticationPrincipal Jwt jwt) {
        return administracion.consultar(jwt);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaFamiliasPlataforma.FamiliaAdministrada crear(
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody SolicitudFamiliaPlataforma solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        return administracion.crear(clave, solicitud, jwt);
    }

    @GetMapping("/{familiaId}/miembros")
    RespuestaMiembrosPlataforma consultarMiembros(@PathVariable java.util.UUID familiaId,
            @AuthenticationPrincipal Jwt jwt) {
        return administracion.consultarMiembros(familiaId, jwt);
    }

    @PostMapping("/{familiaId}/miembros")
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaMiembrosPlataforma.MiembroAdministrado crearMiembro(
            @PathVariable java.util.UUID familiaId,
            @RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody SolicitudMiembroPlataforma solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        return administracion.crearMiembro(familiaId, clave, solicitud, jwt);
    }

    @PatchMapping("/{familiaId}/miembros/{perfilId}")
    RespuestaMiembrosPlataforma.MiembroAdministrado actualizarMiembro(
            @PathVariable java.util.UUID familiaId,
            @PathVariable java.util.UUID perfilId,
            @Valid @RequestBody SolicitudActualizacionMiembroPlataforma solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        return administracion.actualizarMiembro(familiaId, perfilId, solicitud, jwt);
    }
}
