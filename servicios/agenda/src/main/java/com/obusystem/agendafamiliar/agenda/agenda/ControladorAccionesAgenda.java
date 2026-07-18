package com.obusystem.agendafamiliar.agenda.agenda;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/familias/{familiaId}")
public class ControladorAccionesAgenda {
    private final ServicioAccionesAgenda servicio;

    public ControladorAccionesAgenda(ServicioAccionesAgenda servicio) { this.servicio = servicio; }

    @PatchMapping("/{entidad}/{entidadId}/acciones/{accion}")
    RespuestaAccionAgenda actuar(@PathVariable UUID familiaId, @PathVariable String entidad,
            @PathVariable UUID entidadId, @PathVariable AccionAgenda accion,
            @RequestHeader("Idempotency-Key") String clave,
            @RequestBody(required = false) SolicitudAccionAgenda solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        return servicio.actuar(familiaId, entidad, entidadId, accion, clave, solicitud, jwt);
    }
}
