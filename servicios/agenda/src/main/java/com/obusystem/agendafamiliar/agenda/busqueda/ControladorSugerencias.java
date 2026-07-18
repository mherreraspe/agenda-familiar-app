package com.obusystem.agendafamiliar.agenda.busqueda;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/familias/{familiaId}/sugerencias")
public class ControladorSugerencias {
    private final ServicioSugerencias servicio;

    public ControladorSugerencias(ServicioSugerencias servicio) { this.servicio = servicio; }

    @GetMapping
    RespuestaSugerencias consultar(@PathVariable UUID familiaId, @RequestParam("q") String consulta,
            @AuthenticationPrincipal Jwt jwt) {
        return servicio.consultar(familiaId, consulta, jwt);
    }
}
