package com.obusystem.agendafamiliar.autenticacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
        "familia-test.habilitada=true",
        "familia-test.clave=ClaveTemporalSegura2026!"
})
class MigracionesAutenticacionIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void creaLosDosAdultosConCredenciales() {
        Integer cantidad = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE correo IN ('papa@familia.test', 'mama@familia.test')",
                Integer.class);
        assertThat(cantidad).isEqualTo(2);
    }
}
