package com.obusystem.agendafamiliar.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.obusystem.agendafamiliar.agenda.catalogo.RespuestaCatalogo;
import com.obusystem.agendafamiliar.agenda.catalogo.ServicioCatalogo;
import com.obusystem.agendafamiliar.agenda.catalogo.SolicitudesCatalogo;

@Testcontainers
@SpringBootTest
class HorariosTratamientoIT {
    private static final UUID FAMILIA = UUID.fromString("0197f100-0000-7000-8000-000000000001");
    private static final UUID PAPA_USUARIO = UUID.fromString("0197f100-0000-7000-8000-000000000101");
    private static final UUID HIJO = UUID.fromString("0197f100-0000-7000-8000-000000000203");
    private static final UUID PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000201");
    private static final UUID MAMA = UUID.fromString("0197f100-0000-7000-8000-000000000202");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired ServicioCatalogo catalogo;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional
    void conservaVariosHorariosYResponsableAlternativo() {
        UUID id = catalogo.crearTratamiento(FAMILIA, new SolicitudesCatalogo.Tratamiento(
                HIJO, null, "Tratamiento con dos horarios", null, null, null,
                LocalTime.of(8, 0), List.of(LocalTime.of(20, 0)), null,
                LocalDate.now(), LocalDate.now().plusDays(2), PAPA, MAMA), jwt());

        RespuestaCatalogo.TratamientoResumen tratamiento = catalogo.consultar(FAMILIA, jwt()).tratamientos().stream()
                .filter(item -> item.id().equals(id)).findFirst().orElseThrow();
        assertThat(tratamiento.horarios()).containsExactly(LocalTime.of(8, 0), LocalTime.of(20, 0));
        assertThat(tratamiento.responsableAlternativo()).isEqualTo("Mamá");
        assertThat(tratamiento.intervaloHoras()).isNull();
    }

    @Test
    @Transactional
    void materializaIntervalosDesdeElHorarioInicial() {
        UUID id = catalogo.crearTratamiento(FAMILIA, new SolicitudesCatalogo.Tratamiento(
                HIJO, null, "Tratamiento por intervalo", null, null, null,
                LocalTime.of(7, 0), List.of(), 8,
                LocalDate.now(), LocalDate.now().plusDays(1), PAPA, null), jwt());

        Long familiaId = jdbc.queryForObject("SELECT id FROM familias WHERE id_publico=?", Long.class, FAMILIA);
        jdbc.queryForObject("SELECT set_config('agenda.familia_id', ?, true)", String.class, familiaId.toString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ocurrencias_tratamiento o JOIN tratamientos t ON t.id=o.tratamiento_id WHERE t.id_publico=?",
                Integer.class, id)).isGreaterThanOrEqualTo(5);
        assertThat(catalogo.consultar(FAMILIA, jwt()).tratamientos().stream()
                .filter(item -> item.id().equals(id)).findFirst().orElseThrow().intervaloHoras()).isEqualTo(8);
    }

    private Jwt jwt() {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), Map.of("sub", PAPA_USUARIO.toString()));
    }
}
