package com.obusystem.agendafamiliar.autenticacion.acceso;

import java.util.UUID;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class ControladorEnlacesAcceso {
    private final ServicioEnlacesAcceso enlaces;

    public ControladorEnlacesAcceso(ServicioEnlacesAcceso enlaces) {
        this.enlaces = enlaces;
    }

    @PostMapping("/administracion/invitaciones")
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaEnlace.Generado invitar(@RequestHeader("Idempotency-Key") String clave,
            @Valid @RequestBody SolicitudInvitacion solicitud, @AuthenticationPrincipal Jwt jwt) {
        return enlaces.invitar(clave, solicitud, jwt);
    }

    @PostMapping("/administracion/usuarios/{usuarioId}/restablecimientos")
    @ResponseStatus(HttpStatus.CREATED)
    RespuestaEnlace.Generado restablecer(@PathVariable UUID usuarioId,
            @RequestHeader("Idempotency-Key") String clave, @AuthenticationPrincipal Jwt jwt) {
        return enlaces.restablecer(usuarioId, clave, jwt);
    }

    @GetMapping("/administracion/enlaces")
    RespuestaEnlace.Lista consultar(@RequestParam UUID familiaId, @AuthenticationPrincipal Jwt jwt) {
        return enlaces.consultar(familiaId, jwt);
    }

    @GetMapping("/administracion/usuarios")
    RespuestaEnlace.Cuentas consultarCuentas(@RequestParam List<UUID> ids, @AuthenticationPrincipal Jwt jwt) {
        return enlaces.consultarCuentas(ids, jwt);
    }

    @DeleteMapping("/administracion/enlaces/{enlaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revocar(@PathVariable UUID enlaceId, @AuthenticationPrincipal Jwt jwt) {
        enlaces.revocar(enlaceId, jwt);
    }

    @PostMapping("/enlaces/consultar")
    RespuestaEnlace.Publico consultarPublico(@Valid @RequestBody SolicitudToken solicitud) {
        return enlaces.consultarPublico(solicitud.token());
    }

    @PostMapping("/enlaces/consumir")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void consumir(@Valid @RequestBody SolicitudConsumirEnlace solicitud) {
        enlaces.consumir(solicitud);
    }
}
