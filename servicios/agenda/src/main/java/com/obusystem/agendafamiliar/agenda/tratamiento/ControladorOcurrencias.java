package com.obusystem.agendafamiliar.agenda.tratamiento;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.obusystem.agendafamiliar.agenda.sincronizacion.RecursoSincronizacion;
import com.obusystem.agendafamiliar.agenda.sincronizacion.ServicioSincronizacionFamilia;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/familias/{familiaId}")
public class ControladorOcurrencias {
    private final ServicioOcurrencias servicio;
    private final ServicioSincronizacionFamilia sincronizacion;

    public ControladorOcurrencias(ServicioOcurrencias servicio, ServicioSincronizacionFamilia sincronizacion) {
        this.servicio = servicio;
        this.sincronizacion = sincronizacion;
    }

    @GetMapping("/ocurrencias")
    RespuestaOcurrencias consultar(@PathVariable UUID familiaId, @AuthenticationPrincipal Jwt jwt) {
        return servicio.consultar(familiaId, jwt);
    }

    @GetMapping("/revisar")
    RespuestaOcurrencias revisar(@PathVariable UUID familiaId, @AuthenticationPrincipal Jwt jwt) {
        return servicio.consultar(familiaId, jwt);
    }

    @PatchMapping("/ocurrencias/{ocurrenciaId}/estado/{estado}")
    RespuestaOcurrencias.OcurrenciaResumen cambiarEstado(@PathVariable UUID familiaId,
            @PathVariable UUID ocurrenciaId, @PathVariable EstadoOcurrencia estado,
            @RequestHeader("Idempotency-Key") String claveIdempotencia,
            @RequestBody(required = false) SolicitudAccionOcurrencia solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        RespuestaOcurrencias.OcurrenciaResumen respuesta = servicio.cambiarEstado(
                familiaId, ocurrenciaId, estado, claveIdempotencia, solicitud, jwt);
        sincronizacion.publicarIdempotente(familiaId, claveIdempotencia,
                RecursoSincronizacion.HOY, RecursoSincronizacion.SALUD);
        return respuesta;
    }

    @PatchMapping("/revisar/{elementoId}/cerrar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cerrarRevision(@PathVariable UUID familiaId, @PathVariable UUID elementoId,
            @RequestHeader("Idempotency-Key") String claveIdempotencia,
            @AuthenticationPrincipal Jwt jwt) {
        servicio.cerrarRevision(familiaId, elementoId, claveIdempotencia, jwt);
        sincronizacion.publicarIdempotente(familiaId, claveIdempotencia,
                RecursoSincronizacion.HOY, RecursoSincronizacion.SALUD);
    }

    @PatchMapping("/tratamientos/{tratamientoId}/cerrar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void cerrarTratamiento(@PathVariable UUID familiaId, @PathVariable UUID tratamientoId,
            @RequestHeader("Idempotency-Key") String claveIdempotencia,
            @Valid @RequestBody(required = false) SolicitudCierreTratamiento solicitud,
            @AuthenticationPrincipal Jwt jwt) {
        servicio.cerrarTratamiento(familiaId, tratamientoId, claveIdempotencia, solicitud, jwt);
        sincronizacion.publicarIdempotente(familiaId, claveIdempotencia,
                RecursoSincronizacion.HOY, RecursoSincronizacion.SALUD);
    }
}
