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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.obusystem.agendafamiliar.agenda.auditoria.ServicioAuditoria;
import com.obusystem.agendafamiliar.agenda.busqueda.ServicioSugerencias;
import com.obusystem.agendafamiliar.agenda.catalogo.ServicioCatalogo;
import com.obusystem.agendafamiliar.agenda.catalogo.SolicitudesCatalogo;

@Testcontainers
@SpringBootTest
class AuditoriaYBusquedaIT {
    private static final UUID FAMILIA = UUID.fromString("0197f100-0000-7000-8000-000000000001");
    private static final UUID PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000101");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired ServicioCatalogo catalogo;
    @Autowired ServicioAuditoria auditoria;
    @Autowired ServicioSugerencias sugerencias;
    @Autowired JdbcTemplate jdbc;

    @Test
    void guardaLugarRealIndexaSinBloquearYMuestraActorAdulto() throws Exception {
        Instant inicio = Instant.now().plusSeconds(86400);
        UUID evento = catalogo.crearEvento(FAMILIA,
                new SolicitudesCatalogo.Evento(null, "Control pediátrico", "CONTROL", "Centro familiar",
                        "Dirección de prueba", null, inicio, null), jwt());
        catalogo.crearEvento(FAMILIA,
                new SolicitudesCatalogo.Evento(null, "Otra cita", null, "Centro familiar",
                        "Dirección de prueba", null, inicio.plusSeconds(3600), null), jwt());

        int palabras = esperarPalabras(evento);
        assertThat(palabras).isEqualTo(2);
        assertThat(sugerencias.consultar(FAMILIA, "pediatra", jwt()).sugerencias())
                .anySatisfy(sugerencia -> assertThat(sugerencia.entidadId()).isEqualTo(evento));
        assertThat(catalogo.consultar(FAMILIA, jwt()).lugares())
                .anySatisfy(lugar -> {
                    assertThat(lugar.nombre()).isEqualTo("Centro familiar");
                    assertThat(lugar.direccion()).isEqualTo("Dirección de prueba");
                    assertThat(lugar.frecuenciaUso()).isEqualTo(2);
                });
        assertThat(auditoria.consultar(FAMILIA, jwt()).entradas())
                .anySatisfy(entrada -> {
                    assertThat(entrada.entidadId()).isEqualTo(evento);
                    assertThat(entrada.actor()).isEqualTo("Papá");
                    assertThat(entrada.fecha()).isNotNull();
                });
    }

    private int esperarPalabras(UUID evento) throws InterruptedException {
        Long familiaId = jdbc.queryForObject("SELECT id FROM familias WHERE id_publico=?", Long.class, FAMILIA);
        for (int intento = 0; intento < 40; intento++) {
            jdbc.queryForObject("SELECT set_config('agenda.familia_id', ?, false)", String.class, familiaId.toString());
            Integer cantidad = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM palabras_clave WHERE familia_id=? AND entidad_publica_id=?", Integer.class,
                    familiaId, evento);
            if (cantidad != null && cantidad == 2) return cantidad;
            Thread.sleep(50);
        }
        return 0;
    }

    private Jwt jwt() {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), Map.of("sub", PAPA.toString()));
    }
}
