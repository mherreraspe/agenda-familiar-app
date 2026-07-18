package com.obusystem.agendafamiliar.agenda;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.obusystem.agendafamiliar.agenda.catalogo.ServicioCatalogo;

@Testcontainers
@SpringBootTest
class EstadosBotiquinIT {
    private static final UUID FAMILIA = UUID.fromString("0197f100-0000-7000-8000-000000000001");
    private static final UUID PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000101");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired ServicioCatalogo catalogo;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional
    void calculaTodosLosEstadosSinAlterarElDatoClinico() {
        Long familiaId = jdbc.queryForObject("SELECT id FROM familias WHERE id_publico=?", Long.class, FAMILIA);
        jdbc.queryForObject("SELECT set_config('agenda.familia_id', ?, true)", String.class, familiaId.toString());

        actualizar("cantidad=12, estado='DISPONIBLE', fecha_vencimiento=CURRENT_DATE+10", familiaId);
        assertThat(estado()).isEqualTo("POR_VENCER");

        actualizar("fecha_vencimiento=CURRENT_DATE-1", familiaId);
        assertThat(estado()).isEqualTo("VENCIDO");

        actualizar("cantidad=0", familiaId);
        assertThat(estado()).isEqualTo("AGOTADO");

        actualizar("cantidad=12, estado='DESCARTADO', fecha_vencimiento=CURRENT_DATE+120", familiaId);
        assertThat(estado()).isEqualTo("DESCARTADO");
    }

    private void actualizar(String asignaciones, Long familiaId) {
        jdbc.update("UPDATE lotes_medicamento SET " + asignaciones + " WHERE familia_id=?", familiaId);
    }

    private String estado() {
        return catalogo.consultar(FAMILIA, jwt()).medicamentos().getFirst().estado();
    }

    private Jwt jwt() {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), Map.of("sub", PAPA.toString()));
    }
}
