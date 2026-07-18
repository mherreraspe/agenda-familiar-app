package com.obusystem.agendafamiliar.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;

import org.flywaydb.core.Flyway;
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
        Integer horarios = jdbc.queryForObject("SELECT COUNT(*) FROM horarios_tratamiento WHERE familia_id = ?", Integer.class, familiaId);
        Integer lugares = jdbc.queryForObject("SELECT COUNT(*) FROM lugares_familia WHERE familia_id = ?", Integer.class, familiaId);
        assertThat(perfiles).isEqualTo(3);
        assertThat(tareas).isEqualTo(1);
        assertThat(medicamentos).isEqualTo(1);
        assertThat(tratamientos).isEqualTo(1);
        assertThat(eventos).isEqualTo(1);
        assertThat(horarios).isEqualTo(1);
        assertThat(lugares).isEqualTo(1);
    }

    @Test
    @Transactional
    void rlsOcultaOcurrenciasYRevisionDeOtraFamilia() {
        Long familiaId = jdbc.queryForObject(
                "SELECT id FROM familias WHERE id_publico = '0197f100-0000-7000-8000-000000000001'", Long.class);
        jdbc.queryForObject("SELECT set_config('agenda.familia_id', ?, true)", String.class, familiaId.toString());
        Long tratamientoId = jdbc.queryForObject("SELECT id FROM tratamientos WHERE familia_id=? LIMIT 1", Long.class, familiaId);
        jdbc.update("INSERT INTO ocurrencias_tratamiento (id_publico, familia_id, tratamiento_id, programada_en) VALUES (gen_random_uuid(), ?, ?, NOW())",
                familiaId, tratamientoId);
        jdbc.update("INSERT INTO acciones_tratamiento (familia_id, tratamiento_id, clave_idempotencia, accion, actor_publico_id) VALUES (?, ?, 'rls-cierre-1', 'CERRAR', gen_random_uuid())",
                familiaId, tratamientoId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ocurrencias_tratamiento", Integer.class)).isEqualTo(1);

        Long otraFamilia = jdbc.queryForObject("INSERT INTO familias (id_publico, nombre) VALUES (gen_random_uuid(), 'otra') RETURNING id", Long.class);
        jdbc.execute("CREATE ROLE prueba_rls NOLOGIN NOBYPASSRLS");
        jdbc.execute("GRANT USAGE ON SCHEMA public TO prueba_rls");
        jdbc.execute("GRANT SELECT ON ocurrencias_tratamiento, elementos_revision, acciones_tratamiento, lugares_familia, palabras_clave, recurrencias_agenda, acciones_agenda TO prueba_rls");
        jdbc.execute("SET LOCAL ROLE prueba_rls");
        jdbc.queryForObject("SELECT set_config('agenda.familia_id', ?, true)", String.class, otraFamilia.toString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ocurrencias_tratamiento", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM elementos_revision", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM acciones_tratamiento", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM lugares_familia", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM palabras_clave", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM recurrencias_agenda", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM acciones_agenda", Integer.class)).isZero();
    }

    @Test
    void migraConElMismoTipoDeRolSinBypassQueProduccion() throws Exception {
        jdbc.execute("CREATE ROLE migrador_rls LOGIN PASSWORD 'solo-prueba-rls' NOBYPASSRLS");
        jdbc.execute("CREATE SCHEMA migracion_rls AUTHORIZATION migrador_rls");
        String url = POSTGRES.getJdbcUrl() + (POSTGRES.getJdbcUrl().contains("?") ? "&" : "?")
                + "currentSchema=migracion_rls";

        Flyway.configure()
                .dataSource(url, "migrador_rls", "solo-prueba-rls")
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection conexion = DriverManager.getConnection(url, "migrador_rls", "solo-prueba-rls");
                var consulta = conexion.createStatement()) {
            consulta.execute("SELECT set_config('agenda.familia_id', '1', FALSE)");
            try (var resultado = consulta.executeQuery(
                    "SELECT COUNT(*), COUNT(*) FILTER (WHERE nombre_libre IS NULL) FROM tratamientos")) {
                assertThat(resultado.next()).isTrue();
                assertThat(resultado.getInt(1)).isEqualTo(1);
                assertThat(resultado.getInt(2)).isZero();
            }
        }
    }
}
