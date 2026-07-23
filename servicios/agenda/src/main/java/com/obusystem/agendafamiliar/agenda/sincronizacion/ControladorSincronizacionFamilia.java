package com.obusystem.agendafamiliar.agenda.sincronizacion;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/familias/{familiaId}")
public class ControladorSincronizacionFamilia {
    private final ServicioSincronizacionFamilia sincronizacion;

    public ControladorSincronizacionFamilia(ServicioSincronizacionFamilia sincronizacion) {
        this.sincronizacion = sincronizacion;
    }

    @GetMapping(path = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter suscribir(@PathVariable UUID familiaId, @AuthenticationPrincipal Jwt jwt) {
        return sincronizacion.suscribir(familiaId, jwt);
    }
}
