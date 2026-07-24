package com.obusystem.agendafamiliar.agenda;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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

import com.obusystem.agendafamiliar.agenda.catalogo.ServicioCatalogo;
import com.obusystem.agendafamiliar.agenda.catalogo.SolicitudesCatalogo;

@Testcontainers
@SpringBootTest
class SaludSimpleIT {
    private static final UUID FAMILIA = UUID.fromString("0197f100-0000-7000-8000-000000000001");
    private static final UUID USUARIO = UUID.fromString("0197f100-0000-7000-8000-000000000101");
    private static final UUID PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000201");
    private static final UUID HIJO = UUID.fromString("0197f100-0000-7000-8000-000000000203");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired ServicioCatalogo catalogo;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional
    void creaUnaIndicacionParaDosPersonasSinDuplicarlaAlReintentar() {
        var solicitud = new SolicitudesCatalogo.TratamientoMultiple(List.of(PAPA, HIJO), null,
                "Gotas para los ojos", "Lágrimas", "2 gotas", "Ambos ojos", null, null,
                List.of(LocalTime.of(8, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)), null,
                LocalDate.now(), LocalDate.now(), null, null);

        var primero = catalogo.crearTratamientos(FAMILIA, "tratamiento-dos-personas", solicitud, jwt());
        var repetido = catalogo.crearTratamientos(FAMILIA, "tratamiento-dos-personas", solicitud, jwt());

        assertThat(repetido).isEqualTo(primero);
        assertThat(primero.ids()).hasSize(2);
        assertThat(catalogo.consultar(FAMILIA, jwt()).tratamientos().stream()
                .filter(item -> item.grupoId().equals(primero.grupoId())).toList())
                .hasSize(2).allSatisfy(item -> {
                    assertThat(item.nombreMedicamento()).isEqualTo("Lágrimas");
                    assertThat(item.dosisIndicada()).isEqualTo("2 gotas");
                    assertThat(item.horarios()).hasSize(3);
                });
        Long familiaId = jdbc.queryForObject("SELECT id FROM familias WHERE id_publico=?", Long.class, FAMILIA);
        jdbc.queryForObject("SELECT set_config('agenda.familia_id', ?, true)", String.class, familiaId.toString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ocurrencias_tratamiento o JOIN tratamientos t ON t.id=o.tratamiento_id WHERE t.familia_id=? AND t.grupo_publico_id=?",
                Integer.class, familiaId, primero.grupoId())).isEqualTo(6);
    }

    @Test
    @Transactional
    void calculaLaVigenciaPorAperturaYMantieneCadaEnvaseIndependiente() {
        var solicitud = new SolicitudesCatalogo.Medicamento("Gotas", "Frasco", "10 ml",
                BigDecimal.ONE, "frasco", LocalDate.now().plusDays(90), "SIN_ABRIR", null, 30,
                true, 7, true, 3);
        var uno = catalogo.crearMedicamento(FAMILIA, "envase-uno", solicitud, jwt());
        var dos = catalogo.crearMedicamento(FAMILIA, "envase-dos", solicitud, jwt());
        var resumenUno = catalogo.consultar(FAMILIA, jwt()).medicamentos().stream()
                .filter(item -> item.loteId().equals(uno.loteId())).findFirst().orElseThrow();

        catalogo.actualizarEnvase(FAMILIA, uno.loteId(), "abrir-envase",
                new SolicitudesCatalogo.ActualizacionEnvase("ABIERTO", LocalDate.now(), 30,
                        true, 7, true, 3, "DISPONIBLE", resumenUno.version()), jwt());

        var medicamentos = catalogo.consultar(FAMILIA, jwt()).medicamentos();
        var abierto = medicamentos.stream().filter(item -> item.loteId().equals(uno.loteId())).findFirst().orElseThrow();
        var cerrado = medicamentos.stream().filter(item -> item.loteId().equals(dos.loteId())).findFirst().orElseThrow();
        assertThat(abierto.estadoEnvase()).isEqualTo("ABIERTO");
        assertThat(abierto.vigenteHasta()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(abierto.motivoVigencia()).isEqualTo("DESPUES_DE_ABRIR");
        assertThat(cerrado.estadoEnvase()).isEqualTo("SIN_ABRIR");
        assertThat(cerrado.vigenteHasta()).isEqualTo(LocalDate.now().plusDays(90));
    }

    @Test
    void instalaIndicesAlineadosConConsultasFamiliares() {
        assertThat(jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE schemaname='public' AND indexname IN ('tratamientos_familia_grupo_idx','ocurrencias_pendientes_familia_programada_idx','lotes_familia_apertura_limite_idx','lotes_familia_estado_envase_idx')",
                String.class)).containsExactlyInAnyOrder("tratamientos_familia_grupo_idx",
                        "ocurrencias_pendientes_familia_programada_idx", "lotes_familia_apertura_limite_idx",
                        "lotes_familia_estado_envase_idx");
    }

    private Jwt jwt() {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), Map.of("sub", USUARIO.toString()));
    }
}
