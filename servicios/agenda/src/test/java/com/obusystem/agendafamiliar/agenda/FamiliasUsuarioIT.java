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

import com.obusystem.agendafamiliar.agenda.family.ServicioFamiliasUsuario;

@Testcontainers
@SpringBootTest
class FamiliasUsuarioIT {
    private static final UUID PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000101");
    private static final UUID FAMILIA_TEST = UUID.fromString("0197f100-0000-7000-8000-000000000001");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired ServicioFamiliasUsuario familias;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional
    void devuelveSoloFamiliasConMembresiaActivaYAdmiteVarias() {
        UUID segunda = crearFamilia("Herrera Huertas");
        agregarMiembro(segunda, PAPA, "ADULTO", true);
        UUID ajena = crearFamilia("Familia ajena");
        agregarMiembro(ajena, UUID.randomUUID(), "ADULTO", true);
        UUID inactiva = crearFamilia("Familia inactiva");
        agregarMiembro(inactiva, PAPA, "ADULTO", false);

        var respuesta = familias.consultar(jwt(PAPA));

        assertThat(respuesta.familias()).extracting(item -> item.id())
                .containsExactlyInAnyOrder(FAMILIA_TEST, segunda)
                .doesNotContain(ajena, inactiva);
        assertThat(respuesta.familias()).filteredOn(item -> item.id().equals(segunda)).singleElement()
                .satisfies(item -> {
                    assertThat(item.nombre()).isEqualTo("Herrera Huertas");
                    assertThat(item.rol()).isEqualTo("ADULTO");
                });
    }

    @Test
    @Transactional
    void cuentaSinMembresiasNoRecibeFamilias() {
        assertThat(familias.consultar(jwt(UUID.randomUUID())).familias()).isEmpty();
    }

    private UUID crearFamilia(String nombre) {
        return jdbc.queryForObject("INSERT INTO familias (id_publico, nombre) VALUES (gen_random_uuid(), ?) RETURNING id_publico",
                UUID.class, nombre);
    }

    private void agregarMiembro(UUID familiaPublica, UUID usuario, String rol, boolean activo) {
        Long familiaId = jdbc.queryForObject("SELECT id FROM familias WHERE id_publico=?", Long.class, familiaPublica);
        jdbc.queryForObject("SELECT set_config('agenda.familia_id', ?, true)", String.class, familiaId.toString());
        jdbc.update("INSERT INTO miembros_familia (familia_id, usuario_publico_id, rol, activo) VALUES (?, ?, ?, ?)",
                familiaId, usuario, rol, activo);
    }

    private Jwt jwt(UUID usuario) {
        Instant ahora = Instant.now();
        return new Jwt("token", ahora, ahora.plusSeconds(300), Map.of("alg", "none"), Map.of("sub", usuario.toString()));
    }
}
