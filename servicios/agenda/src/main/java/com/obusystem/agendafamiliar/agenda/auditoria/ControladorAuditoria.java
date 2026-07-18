package com.obusystem.agendafamiliar.agenda.auditoria;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/familias/{familiaId}/auditoria")
public class ControladorAuditoria {
    private final ServicioAuditoria servicio;

    public ControladorAuditoria(ServicioAuditoria servicio) { this.servicio = servicio; }

    @GetMapping
    RespuestaAuditoria consultar(@PathVariable UUID familiaId, @AuthenticationPrincipal Jwt jwt) {
        return servicio.consultar(familiaId, jwt);
    }
}
