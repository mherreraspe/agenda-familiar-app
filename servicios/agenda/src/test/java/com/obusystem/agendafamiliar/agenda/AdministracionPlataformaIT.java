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
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.obusystem.agendafamiliar.agenda.administracion.ServicioAdministracionPlataforma;
import com.obusystem.agendafamiliar.agenda.administracion.SolicitudFamiliaPlataforma;

@Testcontainers
@SpringBootTest
class AdministracionPlataformaIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired ServicioAdministracionPlataforma administracion;
    @Autowired JdbcTemplate jdbc;

    @Test
    void creaFamiliaDeFormaIdempotenteYAuditable() {
        var solicitud = new SolicitudFamiliaPlataforma("Familia Rivera", "America/Lima");
        var primera = administracion.crear("familia-rivera-1", solicitud, jwt(true));
        var repetida = administracion.crear("familia-rivera-1", solicitud, jwt(true));

        assertThat(repetida.id()).isEqualTo(primera.id());
        assertThat(administracion.consultar(jwt(true)).familias()).extracting("id").contains(primera.id());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_plataforma WHERE entidad_publica_id=?", Integer.class, primera.id()))
                .isEqualTo(1);
    }

    @Test
    void rechazaUsuariosFamiliaresYZonaInvalida() {
        assertThatThrownBy(() -> administracion.consultar(jwt(false)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> administracion.crear("zona-invalida-1",
                new SolicitudFamiliaPlataforma("Familia", "Lima"), jwt(true)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private Jwt jwt(boolean administrador) {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600), Map.of("alg", "none"),
                Map.of("sub", UUID.randomUUID().toString(), "rol_plataforma",
                        administrador ? "ADMINISTRADOR_PLATAFORMA" : "USUARIO"));
    }
}
