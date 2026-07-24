package com.obusystem.agendafamiliar.agenda.family;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/familias")
public class ControladorFamiliasUsuario {
    private final ServicioFamiliasUsuario familias;

    public ControladorFamiliasUsuario(ServicioFamiliasUsuario familias) {
        this.familias = familias;
    }

    @GetMapping
    RespuestaFamiliasUsuario consultar(@AuthenticationPrincipal Jwt jwt) {
        return familias.consultar(jwt);
    }
}
