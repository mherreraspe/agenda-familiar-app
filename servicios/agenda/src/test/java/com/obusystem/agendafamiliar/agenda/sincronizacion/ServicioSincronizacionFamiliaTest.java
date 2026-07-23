package com.obusystem.agendafamiliar.agenda.sincronizacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.obusystem.agendafamiliar.agenda.family.AccesoFamilia;

class ServicioSincronizacionFamiliaTest {
    private static final UUID FAMILIA_A = UUID.fromString("0197f100-0000-7000-8000-000000000001");
    private static final UUID FAMILIA_B = UUID.fromString("0197f100-0000-7000-8000-000000000002");

    @Test
    void autorizaAntesDeRegistrarLaConexion() {
        AccesoFamilia acceso = mock(AccesoFamilia.class);
        ServicioSincronizacionFamilia servicio = new ServicioSincronizacionFamilia(acceso, SseEmitter::new);
        Jwt jwt = jwt();

        servicio.suscribir(FAMILIA_A, jwt);

        verify(acceso).autorizar(FAMILIA_A, jwt);
        assertThat(servicio.conexionesActivas(FAMILIA_A)).isEqualTo(1);
    }

    @Test
    void noRegistraUnaFamiliaAjena() {
        AccesoFamilia acceso = mock(AccesoFamilia.class);
        Jwt jwt = jwt();
        when(acceso.autorizar(FAMILIA_B, jwt))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Familia no encontrada"));
        ServicioSincronizacionFamilia servicio = new ServicioSincronizacionFamilia(acceso, SseEmitter::new);

        assertThatThrownBy(() -> servicio.suscribir(FAMILIA_B, jwt))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(servicio.conexionesActivas(FAMILIA_B)).isZero();
    }

    @Test
    void publicaSoloEnLasConexionesDeLaFamiliaAfectada() {
        AccesoFamilia acceso = mock(AccesoFamilia.class);
        AtomicInteger enviosA = new AtomicInteger();
        AtomicInteger enviosB = new AtomicInteger();
        AtomicInteger creados = new AtomicInteger();
        ServicioSincronizacionFamilia servicio = new ServicioSincronizacionFamilia(
                acceso, ignorado -> emisorContador(creados.getAndIncrement() == 0 ? enviosA : enviosB));
        servicio.suscribir(FAMILIA_A, jwt());
        servicio.suscribir(FAMILIA_B, jwt());

        servicio.publicar(FAMILIA_A, RecursoSincronizacion.HOY);

        assertThat(enviosA).hasValue(2);
        assertThat(enviosB).hasValue(1);
    }

    @Test
    void conservaTodosLosCambiosPublicadosConcurrentemente() throws Exception {
        AccesoFamilia acceso = mock(AccesoFamilia.class);
        AtomicInteger envios = new AtomicInteger();
        ServicioSincronizacionFamilia servicio = new ServicioSincronizacionFamilia(
                acceso, ignorado -> emisorContador(envios));
        servicio.suscribir(FAMILIA_A, jwt());

        try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tareas = java.util.stream.IntStream.range(0, 40)
                    .mapToObj(indice -> ejecutor.submit(() -> servicio.publicar(
                            FAMILIA_A, RecursoSincronizacion.HOY)))
                    .toList();
            for (var tarea : tareas) tarea.get();
        }

        assertThat(envios).hasValue(41);
    }

    private SseEmitter emisorContador(AtomicInteger contador) {
        return new SseEmitter(0L) {
            @Override
            public void send(SseEventBuilder builder) {
                contador.incrementAndGet();
            }
        };
    }

    private Jwt jwt() {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), Map.of("sub", UUID.randomUUID().toString()));
    }
}
