package com.obusystem.agendafamiliar.agenda.notificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;
import java.util.Base64;

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

import com.obusystem.agendafamiliar.agenda.hoy.ServicioHoy;
import com.obusystem.agendafamiliar.agenda.hoy.SolicitudTarea;

@Testcontainers
@SpringBootTest(properties = "notificaciones.generacion-retardo-inicial-ms=3600000")
class NotificacionesPrivadasIT {
    private static final UUID FAMILIA = UUID.fromString("0197f100-0000-7000-8000-000000000001");
    private static final UUID PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000101");
    private static final UUID MAMA = UUID.fromString("0197f100-0000-7000-8000-000000000102");
    private static final UUID PERFIL_PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000201");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired ServicioNotificaciones notificaciones;
    @Autowired ServicioGeneracionNotificaciones generacion;
    @Autowired ServicioHoy hoy;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional
    void generaAvisoSoloParaElUsuarioRelacionadoYLoDeduplica() {
        notificaciones.consultar(FAMILIA, jwt(PAPA));
        UUID tarea = hoy.crearTarea(FAMILIA,
                new SolicitudTarea("Comprar vendas", null, PERFIL_PAPA, Instant.now()), jwt(PAPA)).id();
        Long familiaInterna = jdbc.queryForObject("SELECT id FROM familias WHERE id_publico=?", Long.class, FAMILIA);

        generacion.generarFamilia(familiaInterna);
        generacion.generarFamilia(familiaInterna);

        RespuestaNotificaciones papa = notificaciones.consultar(FAMILIA, jwt(PAPA));
        RespuestaNotificaciones mama = notificaciones.consultar(FAMILIA, jwt(MAMA));
        assertThat(papa.sinLeer()).isEqualTo(1);
        assertThat(papa.avisos()).singleElement().satisfies(aviso -> {
            assertThat(aviso.tipo()).isEqualTo("TAREA");
            assertThat(aviso.detalle()).isEqualTo("Comprar vendas");
            assertThat(aviso.destino()).contains(tarea.toString());
        });
        assertThat(mama.avisos()).isEmpty();

        notificaciones.marcarLeida(FAMILIA, papa.avisos().getFirst().id(), jwt(PAPA));
        assertThat(notificaciones.consultar(FAMILIA, jwt(PAPA)).sinLeer()).isZero();
    }

    @Test
    @Transactional
    void aislaPreferenciasYDispositivosPorUsuario() {
        var preferencias = notificaciones.guardarPreferencias(FAMILIA,
                new SolicitudPreferenciasNotificacion(true, false, true, false,
                        LocalTime.of(21, 30), LocalTime.of(6, 30)), jwt(PAPA));
        assertThat(preferencias.eventos()).isFalse();
        assertThat(preferencias.silencioDesde()).isEqualTo(LocalTime.of(21, 30));

        var dispositivo = notificaciones.registrarDispositivo(FAMILIA,
                new SolicitudSuscripcionPush("https://fcm.googleapis.com/fcm/send/device-1",
                        Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[65]),
                        Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[16]), "iPhone de Papá"),
                jwt(PAPA));
        assertThat(dispositivo.activo()).isTrue();
        assertThat(notificaciones.consultar(FAMILIA, jwt(MAMA)).dispositivos()).isEmpty();
        assertThatThrownBy(() -> notificaciones.revocarDispositivo(FAMILIA, dispositivo.id(), jwt(MAMA)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);

        notificaciones.revocarDispositivo(FAMILIA, dispositivo.id(), jwt(PAPA));
        assertThat(notificaciones.consultar(FAMILIA, jwt(PAPA)).dispositivos().getFirst().activo()).isFalse();
    }

    private Jwt jwt(UUID usuario) {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), Map.of("sub", usuario.toString()));
    }
}
