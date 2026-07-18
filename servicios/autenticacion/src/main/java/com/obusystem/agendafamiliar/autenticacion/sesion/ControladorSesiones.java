package com.obusystem.agendafamiliar.autenticacion.sesion;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class ControladorSesiones {
    private final ServicioSesiones sesiones;

    public ControladorSesiones(ServicioSesiones sesiones) {
        this.sesiones = sesiones;
    }

    @PostMapping("/iniciar-sesion")
    ResponseEntity<RespuestaSesion> iniciar(@Valid @RequestBody SolicitudInicioSesion solicitud) {
        return ResponseEntity.ok(sesiones.iniciar(solicitud));
    }
}
