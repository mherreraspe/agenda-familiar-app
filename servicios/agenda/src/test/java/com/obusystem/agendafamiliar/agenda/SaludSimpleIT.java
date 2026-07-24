package com.obusystem.agendafamiliar.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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
    void editaElGrupoActivoConservaTomasResueltasYRegeneraSoloPendientesFuturas() {
        var creado = catalogo.crearTratamientos(FAMILIA, "tratamiento-para-editar",
                new SolicitudesCatalogo.TratamientoMultiple(List.of(PAPA, HIJO), null,
                        "Gotas", null, "1 gota", null, null, null,
                        List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)), null,
                        LocalDate.now(), LocalDate.now().plusDays(2), null, null), jwt());
        Long familiaId = jdbc.queryForObject("SELECT id FROM familias WHERE id_publico=?", Long.class, FAMILIA);
        jdbc.queryForObject("SELECT set_config('agenda.familia_id', ?, true)", String.class, familiaId.toString());
        UUID ocurrenciaResuelta = jdbc.queryForObject("SELECT o.id_publico FROM ocurrencias_tratamiento o JOIN tratamientos t ON t.id=o.tratamiento_id WHERE t.familia_id=? AND t.grupo_publico_id=? ORDER BY o.programada_en LIMIT 1",
                UUID.class, familiaId, creado.grupoId());
        jdbc.update("UPDATE ocurrencias_tratamiento SET estado='TOMADA', resuelta_en=NOW() WHERE familia_id=? AND id_publico=?",
                familiaId, ocurrenciaResuelta);

        var actualizacion = new SolicitudesCatalogo.ActualizacionTratamiento(null, "Gotas corregidas",
                "Lágrimas", "2 gotas", "Ambos ojos", "Agitar antes", null,
                List.of(LocalTime.of(10, 0), LocalTime.of(18, 0)), null,
                LocalDate.now(), LocalDate.now().plusDays(3), null, null);
        catalogo.actualizarTratamiento(FAMILIA, creado.grupoId(), "editar-tratamiento-1", actualizacion, jwt());
        catalogo.actualizarTratamiento(FAMILIA, creado.grupoId(), "editar-tratamiento-1", actualizacion, jwt());

        assertThat(catalogo.consultar(FAMILIA, jwt()).tratamientos().stream()
                .filter(item -> item.grupoId().equals(creado.grupoId())).toList())
                .hasSize(2).allSatisfy(item -> {
                    assertThat(item.medicamento()).isEqualTo("Gotas corregidas");
                    assertThat(item.dosisIndicada()).isEqualTo("2 gotas");
                    assertThat(item.horarios()).containsExactly(LocalTime.of(10, 0), LocalTime.of(18, 0));
                });
        assertThat(jdbc.queryForObject("SELECT estado FROM ocurrencias_tratamiento WHERE familia_id=? AND id_publico=?",
                String.class, familiaId, ocurrenciaResuelta)).isEqualTo("TOMADA");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ocurrencias_tratamiento o JOIN tratamientos t ON t.id=o.tratamiento_id JOIN horarios_tratamiento h ON h.id=o.horario_id WHERE t.familia_id=? AND t.grupo_publico_id=? AND o.estado='PENDIENTE' AND o.programada_en>=NOW() AND NOT h.activo",
                Integer.class, familiaId, creado.grupoId())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE familia_id=? AND entidad='TRATAMIENTO' AND entidad_publica_id=? AND operacion='ACTUALIZAR'",
                Integer.class, familiaId, creado.grupoId())).isEqualTo(1);

        jdbc.update("UPDATE tratamientos SET estado='CERRADO' WHERE familia_id=? AND grupo_publico_id=?", familiaId, creado.grupoId());
        assertThatThrownBy(() -> catalogo.actualizarTratamiento(FAMILIA, creado.grupoId(),
                "editar-tratamiento-cerrado", actualizacion, jwt()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
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
