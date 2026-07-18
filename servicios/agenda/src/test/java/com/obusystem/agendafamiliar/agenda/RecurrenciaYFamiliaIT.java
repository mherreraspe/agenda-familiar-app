package com.obusystem.agendafamiliar.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.obusystem.agendafamiliar.agenda.agenda.AccionAgenda;
import com.obusystem.agendafamiliar.agenda.agenda.RespuestaAccionAgenda;
import com.obusystem.agendafamiliar.agenda.agenda.ServicioAccionesAgenda;
import com.obusystem.agendafamiliar.agenda.agenda.SolicitudAccionAgenda;
import com.obusystem.agendafamiliar.agenda.catalogo.ServicioCatalogo;
import com.obusystem.agendafamiliar.agenda.catalogo.SolicitudesCatalogo;
import com.obusystem.agendafamiliar.agenda.family.ServicioAdministracionFamilia;
import com.obusystem.agendafamiliar.agenda.family.SolicitudPerfil;
import com.obusystem.agendafamiliar.agenda.hoy.RespuestaHoy;
import com.obusystem.agendafamiliar.agenda.hoy.ServicioHoy;
import com.obusystem.agendafamiliar.agenda.hoy.SolicitudTarea;
import com.obusystem.agendafamiliar.agenda.recurrencia.SolicitudRecurrencia;
import com.obusystem.agendafamiliar.agenda.recurrencia.SolicitudRecurrencia.Frecuencia;

@Testcontainers
@SpringBootTest
class RecurrenciaYFamiliaIT {
    private static final UUID FAMILIA = UUID.fromString("0197f100-0000-7000-8000-000000000001");
    private static final UUID PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000101");
    private static final UUID MAMA = UUID.fromString("0197f100-0000-7000-8000-000000000102");
    private static final UUID PERFIL_PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000201");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired ServicioHoy hoy;
    @Autowired ServicioCatalogo catalogo;
    @Autowired ServicioAccionesAgenda acciones;
    @Autowired ServicioAdministracionFamilia administracion;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional
    void materializaSerieDeTareasYReprogramaSinPerderLaOriginal() {
        Instant inicio = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        RespuestaHoy.TareaResumen primera = hoy.crearTarea(FAMILIA,
                new SolicitudTarea("Serie semanal V7", "Prueba", PERFIL_PAPA, inicio,
                        new SolicitudRecurrencia(Frecuencia.SEMANAL, 1, inicio.plus(14, ChronoUnit.DAYS))), jwt(PAPA));

        Long recurrenciaId = jdbc.queryForObject("SELECT recurrencia_id FROM tareas WHERE id_publico=?", Long.class, primera.id());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tareas WHERE recurrencia_id=?", Integer.class, recurrenciaId)).isEqualTo(3);

        Instant nuevaFecha = inicio.plus(3, ChronoUnit.DAYS);
        RespuestaAccionAgenda nueva = acciones.actuar(FAMILIA, "tareas", primera.id(), AccionAgenda.REPROGRAMAR,
                "v7-tarea-reprogramar", new SolicitudAccionAgenda(nuevaFecha), jwt(PAPA));
        RespuestaAccionAgenda repetida = acciones.actuar(FAMILIA, "tareas", primera.id(), AccionAgenda.REPROGRAMAR,
                "v7-tarea-reprogramar", new SolicitudAccionAgenda(nuevaFecha), jwt(PAPA));

        assertThat(repetida.id()).isEqualTo(nueva.id());
        assertThat(jdbc.queryForObject("SELECT estado FROM tareas WHERE id_publico=?", String.class, primera.id()))
                .isEqualTo("REPROGRAMADA");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tareas nueva JOIN tareas original ON original.id=nueva.tarea_origen_id WHERE original.id_publico=? AND nueva.id_publico=? AND nueva.estado='PENDIENTE'",
                Integer.class, primera.id(), nueva.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM acciones_agenda WHERE clave_idempotencia='v7-tarea-reprogramar'", Integer.class)).isEqualTo(1);
    }

    @Test
    @Transactional
    void materializaEventosYOmitirUnaInstanciaNoAfectaLasDemas() {
        Instant inicio = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        UUID primero = catalogo.crearEvento(FAMILIA,
                new SolicitudesCatalogo.Evento(PERFIL_PAPA, "Evento diario V7", "OTRO", null, null, null,
                        inicio, inicio.plus(1, ChronoUnit.HOURS),
                        new SolicitudRecurrencia(Frecuencia.DIARIA, 1, inicio.plus(2, ChronoUnit.DAYS))), jwt(PAPA));
        Long recurrenciaId = jdbc.queryForObject("SELECT recurrencia_id FROM eventos WHERE id_publico=?", Long.class, primero);

        acciones.actuar(FAMILIA, "eventos", primero, AccionAgenda.OMITIR, "v7-evento-omitir", null, jwt(PAPA));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM eventos WHERE recurrencia_id=?", Integer.class, recurrenciaId)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM eventos WHERE recurrencia_id=? AND estado='PROGRAMADO'", Integer.class, recurrenciaId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT estado FROM eventos WHERE id_publico=?", String.class, primero)).isEqualTo("OMITIDO");
    }

    @Test
    void soloUnaTransicionCompetidoraPuedeResolverLaTarea() throws Exception {
        Instant fecha = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
        UUID tarea = hoy.crearTarea(FAMILIA, new SolicitudTarea("Carrera V7 " + UUID.randomUUID(), null,
                PERFIL_PAPA, fecha), jwt(PAPA)).id();
        CountDownLatch salida = new CountDownLatch(1);
        try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            var omitir = ejecutor.submit(() -> competir(salida, tarea, AccionAgenda.OMITIR, "v7-carrera-omitir", null));
            var reprogramar = ejecutor.submit(() -> competir(salida, tarea, AccionAgenda.REPROGRAMAR,
                    "v7-carrera-reprogramar", fecha.plus(1, ChronoUnit.DAYS)));
            salida.countDown();
            assertThat(List.of(omitir.get(), reprogramar.get())).containsExactlyInAnyOrder("OK", "CONFLICT");
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM acciones_agenda WHERE entidad_publica_id=?", Integer.class, tarea)).isEqualTo(1);
    }

    @Test
    @Transactional
    void restringeAdministracionEIdorYProtegeAlUltimoAdministrador() {
        assertThatThrownBy(() -> administracion.crear(FAMILIA,
                new SolicitudPerfil("No autorizado", "DEPENDIENTE", null, null, null, null, true), jwt(MAMA)))
                .isInstanceOf(ResponseStatusException.class).extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        var dependiente = administracion.crear(FAMILIA,
                new SolicitudPerfil("Dependiente V7", "DEPENDIENTE", "#123456", "Hija", null, null, true), jwt(PAPA));
        assertThat(dependiente.tipo()).isEqualTo("DEPENDIENTE");

        assertThatThrownBy(() -> administracion.actualizar(FAMILIA, PERFIL_PAPA,
                new SolicitudPerfil("Papá", "ADULTO", "#315b4c", "Papá", PAPA, "ADULTO", true), jwt(PAPA)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("administrador activo");

        UUID otraFamilia = jdbc.queryForObject("INSERT INTO familias (id_publico, nombre) VALUES (gen_random_uuid(), 'Ajena V7') RETURNING id_publico", UUID.class);
        assertThatThrownBy(() -> acciones.actuar(otraFamilia, "tareas", dependiente.id(), AccionAgenda.OMITIR,
                "v7-idor", null, jwt(PAPA))).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Familia no encontrada");
    }

    private String competir(CountDownLatch salida, UUID tarea, AccionAgenda accion, String clave, Instant fecha) throws InterruptedException {
        salida.await();
        try {
            acciones.actuar(FAMILIA, "tareas", tarea, accion, clave,
                    fecha == null ? null : new SolicitudAccionAgenda(fecha), jwt(PAPA));
            return "OK";
        } catch (ResponseStatusException error) {
            return error.getStatusCode().equals(HttpStatus.CONFLICT) ? "CONFLICT" : error.getStatusCode().toString();
        }
    }

    private Jwt jwt(UUID usuario) {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), Map.of("sub", usuario.toString()));
    }
}
