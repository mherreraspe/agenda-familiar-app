package com.obusystem.agendafamiliar.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class MigracionesAgendaIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @Transactional
    void creaFamiliaTestConTresPerfilesYAislamientoRls() {
        Long familiaId = jdbc.queryForObject(
                "SELECT id FROM familias WHERE id_publico = '0197f100-0000-7000-8000-000000000001'", Long.class);
        jdbc.queryForObject("SELECT set_config('agenda.familia_id', ?, true)", String.class, familiaId.toString());
        Integer perfiles = jdbc.queryForObject("SELECT COUNT(*) FROM perfiles WHERE familia_id = ?", Integer.class, familiaId);
        Integer tareas = jdbc.queryForObject("SELECT COUNT(*) FROM tareas WHERE familia_id = ?", Integer.class, familiaId);
        Integer medicamentos = jdbc.queryForObject("SELECT COUNT(*) FROM medicamentos WHERE familia_id = ?", Integer.class, familiaId);
        Integer tratamientos = jdbc.queryForObject("SELECT COUNT(*) FROM tratamientos WHERE familia_id = ?", Integer.class, familiaId);
        Integer eventos = jdbc.queryForObject("SELECT COUNT(*) FROM eventos WHERE familia_id = ?", Integer.class, familiaId);
        assertThat(perfiles).isEqualTo(3);
        assertThat(tareas).isEqualTo(1);
        assertThat(medicamentos).isEqualTo(1);
        assertThat(tratamientos).isEqualTo(1);
        assertThat(eventos).isEqualTo(1);
    }
}
