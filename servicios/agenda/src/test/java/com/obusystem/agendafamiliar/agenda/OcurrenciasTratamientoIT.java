package com.obusystem.agendafamiliar.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.obusystem.agendafamiliar.agenda.tratamiento.EstadoOcurrencia;
import com.obusystem.agendafamiliar.agenda.tratamiento.RespuestaOcurrencias;
import com.obusystem.agendafamiliar.agenda.tratamiento.ServicioOcurrencias;
import com.obusystem.agendafamiliar.agenda.tratamiento.SolicitudAccionOcurrencia;

@Testcontainers
@SpringBootTest
class OcurrenciasTratamientoIT {
    private static final UUID FAMILIA = UUID.fromString("0197f100-0000-7000-8000-000000000001");
    private static final UUID PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000101");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired ServicioOcurrencias servicio;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional
    void materializaUnaSolaVezYCambiarEstadoEsIdempotente() {
        RespuestaOcurrencias primera = servicio.consultar(FAMILIA, jwt());
        int cantidad = primera.ocurrencias().size();
        assertThat(cantidad).isGreaterThan(0);
        assertThat(servicio.consultar(FAMILIA, jwt()).ocurrencias()).hasSize(cantidad);

        RespuestaOcurrencias.OcurrenciaResumen ocurrencia = primera.ocurrencias().getFirst();
        RespuestaOcurrencias.OcurrenciaResumen tomada = servicio.cambiarEstado(FAMILIA, ocurrencia.id(),
                EstadoOcurrencia.TOMADA, "prueba-tomada-1", null, jwt());
        RespuestaOcurrencias.OcurrenciaResumen repetida = servicio.cambiarEstado(FAMILIA, ocurrencia.id(),
                EstadoOcurrencia.TOMADA, "prueba-tomada-1", null, jwt());

        assertThat(tomada.estado()).isEqualTo("TOMADA");
        assertThat(repetida.estado()).isEqualTo("TOMADA");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM acciones_ocurrencia WHERE clave_idempotencia='prueba-tomada-1'", Integer.class)).isEqualTo(1);
        assertThatThrownBy(() -> servicio.cambiarEstado(FAMILIA, ocurrencia.id(), EstadoOcurrencia.OMITIDA,
                "prueba-tomada-1", null, jwt())).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("idempotencia");
        assertThatThrownBy(() -> servicio.cambiarEstado(FAMILIA, ocurrencia.id(), EstadoOcurrencia.OMITIDA,
                "prueba-tomada-competidora", null, jwt())).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ya fue resuelta");
    }

    @Test
    @Transactional
    void posponerConservaLaOriginalYCreaUnaNuevaPendiente() {
        RespuestaOcurrencias respuesta = servicio.consultar(FAMILIA, jwt());
        RespuestaOcurrencias.OcurrenciaResumen original = respuesta.ocurrencias().getLast();
        Instant nuevaFecha = Instant.now().plusSeconds(172800).truncatedTo(java.time.temporal.ChronoUnit.MICROS);

        RespuestaOcurrencias.OcurrenciaResumen pospuesta = servicio.cambiarEstado(FAMILIA, original.id(),
                EstadoOcurrencia.POSPUESTA, "prueba-posponer-1", new SolicitudAccionOcurrencia(nuevaFecha), jwt());

        assertThat(pospuesta.estado()).isEqualTo("POSPUESTA");
        assertThat(pospuesta.pospuestaA()).isEqualTo(nuevaFecha);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ocurrencias_tratamiento WHERE programada_en=? AND estado='PENDIENTE'",
                Integer.class, java.sql.Timestamp.from(nuevaFecha))).isEqualTo(1);
    }

    @Test
    @Transactional
    void reprogramarEnlazaLaNuevaOcurrenciaYExponeElActor() {
        RespuestaOcurrencias respuesta = servicio.consultar(FAMILIA, jwt());
        RespuestaOcurrencias.OcurrenciaResumen original = respuesta.ocurrencias().getLast();
        Instant nuevaFecha = Instant.now().plusSeconds(259200).truncatedTo(java.time.temporal.ChronoUnit.MICROS);

        RespuestaOcurrencias.OcurrenciaResumen reprogramada = servicio.cambiarEstado(FAMILIA, original.id(),
                EstadoOcurrencia.REPROGRAMADA, "prueba-reprogramar-1",
                new SolicitudAccionOcurrencia(nuevaFecha), jwt());

        assertThat(reprogramada.estado()).isEqualTo("REPROGRAMADA");
        assertThat(reprogramada.resueltaPorNombre()).isEqualTo("Papá");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ocurrencias_tratamiento nueva JOIN ocurrencias_tratamiento original ON original.id=nueva.ocurrencia_origen_id WHERE original.id_publico=? AND nueva.programada_en=? AND nueva.estado='PENDIENTE'",
                Integer.class, original.id(), java.sql.Timestamp.from(nuevaFecha))).isEqualTo(1);
    }

    @Test
    @Transactional
    void permiteCerrarAnticipadamenteConHistorialIdempotente() {
        RespuestaOcurrencias respuesta = servicio.consultar(FAMILIA, jwt());
        UUID tratamiento = respuesta.ocurrencias().getFirst().tratamientoId();

        servicio.cerrarTratamiento(FAMILIA, tratamiento, "cerrar-anticipado-1",
                new com.obusystem.agendafamiliar.agenda.tratamiento.SolicitudCierreTratamiento("Cambio indicado por la familia"), jwt());
        servicio.cerrarTratamiento(FAMILIA, tratamiento, "cerrar-anticipado-1",
                new com.obusystem.agendafamiliar.agenda.tratamiento.SolicitudCierreTratamiento("Cambio indicado por la familia"), jwt());

        assertThat(jdbc.queryForObject("SELECT estado FROM tratamientos WHERE id_publico=?", String.class, tratamiento))
                .isEqualTo("CERRADO");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ocurrencias_tratamiento o JOIN tratamientos t ON t.id=o.tratamiento_id WHERE t.id_publico=? AND o.estado='PENDIENTE'",
                Integer.class, tratamiento)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM acciones_tratamiento WHERE clave_idempotencia='cerrar-anticipado-1'",
                Integer.class)).isEqualTo(1);
    }

    @Test
    @Transactional
    void rechazaAccionesSobreUnaFamiliaAjena() {
        RespuestaOcurrencias.OcurrenciaResumen ocurrencia = servicio.consultar(FAMILIA, jwt()).ocurrencias().getFirst();
        UUID otraFamilia = jdbc.queryForObject(
                "INSERT INTO familias (id_publico, nombre) VALUES (gen_random_uuid(), 'Familia ajena') RETURNING id_publico",
                UUID.class);

        assertThatThrownBy(() -> servicio.cambiarEstado(otraFamilia, ocurrencia.id(),
                EstadoOcurrencia.REPROGRAMADA, "idor-reprogramar-1",
                new SolicitudAccionOcurrencia(Instant.now().plusSeconds(7200)), jwt()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Familia no encontrada");
        assertThatThrownBy(() -> servicio.cerrarTratamiento(otraFamilia, ocurrencia.tratamientoId(),
                "idor-cerrar-1", null, jwt()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Familia no encontrada");
    }

    @Test
    @Transactional
    void revisarCierraTratamientoFinalizadoDeFormaIdempotente() {
        Long familiaId = jdbc.queryForObject("SELECT id FROM familias WHERE id_publico=?", Long.class, FAMILIA);
        jdbc.queryForObject("SELECT set_config('agenda.familia_id', ?, true)", String.class, familiaId.toString());
        jdbc.update("UPDATE tratamientos SET fecha_inicio=CURRENT_DATE-2, fecha_fin=CURRENT_DATE-1 WHERE familia_id=?", familiaId);
        RespuestaOcurrencias respuesta = servicio.consultar(FAMILIA, jwt());
        RespuestaOcurrencias.ElementoRevision elemento = respuesta.revisar().stream()
                .filter(item -> item.origen().equals("TRATAMIENTO")).findFirst().orElseThrow();

        servicio.cerrarRevision(FAMILIA, elemento.id(), "cerrar-tratamiento-1", jwt());
        servicio.cerrarRevision(FAMILIA, elemento.id(), "cerrar-tratamiento-1", jwt());

        assertThat(jdbc.queryForObject("SELECT estado FROM tratamientos WHERE familia_id=? LIMIT 1", String.class, familiaId))
                .isEqualTo("CERRADO");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM acciones_revision WHERE clave_idempotencia='cerrar-tratamiento-1'", Integer.class))
                .isEqualTo(1);
    }

    private Jwt jwt() {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), Map.of("sub", PAPA.toString()));
    }
}
