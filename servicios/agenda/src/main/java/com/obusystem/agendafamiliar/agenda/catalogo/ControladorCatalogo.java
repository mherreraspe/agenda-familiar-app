package com.obusystem.agendafamiliar.agenda.catalogo;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obusystem.agendafamiliar.agenda.sincronizacion.RecursoSincronizacion;
import com.obusystem.agendafamiliar.agenda.sincronizacion.ServicioSincronizacionFamilia;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/familias/{familiaId}")
public class ControladorCatalogo {
    private final ServicioCatalogo catalogo;
    private final ServicioSincronizacionFamilia sincronizacion;

    public ControladorCatalogo(ServicioCatalogo catalogo, ServicioSincronizacionFamilia sincronizacion) {
        this.catalogo = catalogo;
        this.sincronizacion = sincronizacion;
    }

    @GetMapping("/catalogo")
    RespuestaCatalogo consultar(@PathVariable UUID familiaId, @AuthenticationPrincipal Jwt jwt) {
        return catalogo.consultar(familiaId, jwt);
    }

    @PostMapping("/medicamentos")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> crearMedicamento(@PathVariable UUID familiaId,
            @Valid @RequestBody SolicitudesCatalogo.Medicamento solicitud, @AuthenticationPrincipal Jwt jwt) {
        UUID id = catalogo.crearMedicamento(familiaId, solicitud, jwt);
        sincronizacion.publicar(familiaId, RecursoSincronizacion.SALUD);
        return Map.of("id", id);
    }

    @PostMapping("/tratamientos")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> crearTratamiento(@PathVariable UUID familiaId,
            @Valid @RequestBody SolicitudesCatalogo.Tratamiento solicitud, @AuthenticationPrincipal Jwt jwt) {
        UUID id = catalogo.crearTratamiento(familiaId, solicitud, jwt);
        sincronizacion.publicar(familiaId, RecursoSincronizacion.HOY, RecursoSincronizacion.SALUD);
        return Map.of("id", id);
    }

    @PostMapping("/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> crearEvento(@PathVariable UUID familiaId,
            @Valid @RequestBody SolicitudesCatalogo.Evento solicitud, @AuthenticationPrincipal Jwt jwt) {
        UUID id = catalogo.crearEvento(familiaId, solicitud, jwt);
        sincronizacion.publicar(familiaId, RecursoSincronizacion.HOY, RecursoSincronizacion.AGENDA);
        return Map.of("id", id);
    }
}
