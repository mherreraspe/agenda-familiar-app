package com.obusystem.agendafamiliar.agenda.objetos;

import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.obusystem.agendafamiliar.agenda.sincronizacion.RecursoSincronizacion;
import com.obusystem.agendafamiliar.agenda.sincronizacion.ServicioSincronizacionFamilia;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/familias/{familiaId}/objetos")
public class ControladorObjetos {
    private final ServicioObjetos objetos;
    private final ServicioSincronizacionFamilia sincronizacion;

    public ControladorObjetos(ServicioObjetos objetos, ServicioSincronizacionFamilia sincronizacion) {
        this.objetos = objetos;
        this.sincronizacion = sincronizacion;
    }

    @GetMapping
    RespuestaObjetos consultar(@PathVariable UUID familiaId, @RequestParam(defaultValue = "") String q,
            @AuthenticationPrincipal Jwt jwt) {
        return objetos.consultar(familiaId, q, jwt);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, UUID> crear(@PathVariable UUID familiaId, @RequestHeader("Idempotency-Key") String claveIdempotencia,
            @Valid @RequestBody SolicitudObjeto solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        UUID id = objetos.crear(familiaId, claveIdempotencia, solicitud, jwt);
        sincronizacion.publicar(familiaId, RecursoSincronizacion.OBJETOS);
        return Map.of("id", id);
    }

    @PatchMapping("/{objetoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void actualizar(@PathVariable UUID familiaId, @PathVariable UUID objetoId,
            @Valid @RequestBody SolicitudObjeto solicitud, @AuthenticationPrincipal Jwt jwt) {
        objetos.actualizar(familiaId, objetoId, solicitud, jwt);
        sincronizacion.publicar(familiaId, RecursoSincronizacion.OBJETOS);
    }
}
