package com.obusystem.agendafamiliar.autenticacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.obusystem.agendafamiliar.autenticacion.acceso.ServicioEnlacesAcceso;
import com.obusystem.agendafamiliar.autenticacion.acceso.SolicitudConsumirEnlace;
import com.obusystem.agendafamiliar.autenticacion.acceso.SolicitudInvitacion;
import com.obusystem.agendafamiliar.autenticacion.sesion.ServicioSesiones;
import com.obusystem.agendafamiliar.autenticacion.sesion.SolicitudInicioSesion;
import com.obusystem.agendafamiliar.autenticacion.usuario.RepositorioUsuarios;
import com.obusystem.agendafamiliar.autenticacion.usuario.Usuario;

@Testcontainers
@SpringBootTest
class EnlacesAccesoIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired ServicioEnlacesAcceso enlaces;
    @Autowired ServicioSesiones sesiones;
    @Autowired RepositorioUsuarios usuarios;
    @Autowired PasswordEncoder claves;
    @Autowired JdbcTemplate jdbc;

    @Test
    void invitacionEsIdempotenteUnicaYNoGuardaElToken() {
        UUID usuarioId = UUID.randomUUID();
        var solicitud = new SolicitudInvitacion(usuarioId, UUID.randomUUID(), "Familia Rivera", "persona@example.com");
        Jwt administrador = jwt(true);
        var primero = enlaces.invitar("invitar-persona-1", solicitud, administrador);
        var repetido = enlaces.invitar("invitar-persona-1", solicitud, administrador);
        String token = token(primero.enlace());

        assertThat(repetido.enlace()).isEqualTo(primero.enlace());
        assertThat(jdbc.queryForObject("SELECT token_hash FROM enlaces_acceso WHERE id=?", String.class, primero.id()))
                .hasSize(64).isNotEqualTo(token);
        assertThat(enlaces.consultarPublico(token).correo()).isEqualTo("p***@example.com");

        enlaces.consumir(new SolicitudConsumirEnlace(token, "UnaClaveSegura2026!"));
        assertThat(usuarios.findByIdPublico(usuarioId)).isPresent();
        assertThat(claves.matches("UnaClaveSegura2026!", usuarios.findByIdPublico(usuarioId).orElseThrow().getClaveHash())).isTrue();
        assertThatThrownBy(() -> enlaces.consumir(new SolicitudConsumirEnlace(token, "OtraClaveSegura2026!")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_acceso WHERE entidad_publica_id=?", Integer.class, primero.id()))
                .isEqualTo(2);
    }

    @Test
    void restablecimientoCambiaClaveYRevocaSesiones() {
        UUID usuarioId = UUID.randomUUID();
        usuarios.saveAndFlush(new Usuario(usuarioId, "recuperar@example.com", claves.encode("ClaveAnterior2026!")));
        var sesion = sesiones.iniciar(new SolicitudInicioSesion("recuperar@example.com", "ClaveAnterior2026!"));
        var generado = enlaces.restablecer(usuarioId, "restablecer-1", jwt(true));

        enlaces.consumir(new SolicitudConsumirEnlace(token(generado.enlace()), "ClaveNuevaSegura2026!"));

        assertThat(claves.matches("ClaveNuevaSegura2026!", usuarios.findByIdPublico(usuarioId).orElseThrow().getClaveHash())).isTrue();
        assertThat(jdbc.queryForObject("SELECT revocado_en IS NOT NULL FROM sesiones_refresh WHERE id=?", Boolean.class, sesion.sesionId())).isTrue();
        assertThatThrownBy(() -> sesiones.iniciar(new SolicitudInicioSesion("recuperar@example.com", "ClaveAnterior2026!")))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(sesiones.iniciar(new SolicitudInicioSesion("recuperar@example.com", "ClaveNuevaSegura2026!")).respuesta().usuarioId())
                .isEqualTo(usuarioId);
    }

    @Test
    void recuperacionActivaAdministradorIndependienteYRetiraRolDePrueba() {
        UUID usuarioId = UUID.randomUUID();
        UUID papaId = UUID.fromString("0197f100-0000-7000-8000-000000000101");
        Usuario papa = usuarios.saveAndFlush(new Usuario(papaId, "papa@familia.test",
                claves.encode("ClaveTemporalSegura2026!")));
        Usuario administradorReal = usuarios.saveAndFlush(new Usuario(usuarioId, "propietario@example.com",
                claves.encode("ClaveInutilizable2026!")));
        jdbc.update("UPDATE usuarios SET estado='PENDIENTE', rol_plataforma='ADMINISTRADOR_PLATAFORMA' WHERE id=?",
                administradorReal.getId());
        jdbc.update("UPDATE usuarios SET rol_plataforma='ADMINISTRADOR_PLATAFORMA' WHERE id_publico=?",
                papa.getIdPublico());
        var generado = enlaces.restablecer(usuarioId, "activar-admin-real-1", jwt(true));

        enlaces.consumir(new SolicitudConsumirEnlace(token(generado.enlace()), "ClavePropietario2026!"));

        assertThat(jdbc.queryForObject("SELECT estado FROM usuarios WHERE id_publico=?", String.class, usuarioId))
                .isEqualTo("ACTIVO");
        assertThat(jdbc.queryForObject("SELECT rol_plataforma FROM usuarios WHERE id_publico=?", String.class, usuarioId))
                .isEqualTo("ADMINISTRADOR_PLATAFORMA");
        assertThat(jdbc.queryForObject("SELECT rol_plataforma FROM usuarios WHERE id_publico=?", String.class,
                papa.getIdPublico())).isEqualTo("USUARIO");
        assertThat(sesiones.iniciar(new SolicitudInicioSesion("propietario@example.com", "ClavePropietario2026!")).respuesta()
                .rolPlataforma()).isEqualTo("ADMINISTRADOR_PLATAFORMA");
    }

    @Test
    void rechazaAdministradorFalsoYEnlaceVencido() {
        var solicitud = new SolicitudInvitacion(UUID.randomUUID(), UUID.randomUUID(), "Familia", "vencido@example.com");
        assertThatThrownBy(() -> enlaces.invitar("sin-permiso", solicitud, jwt(false)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        var generado = enlaces.invitar("vencido-1", solicitud, jwt(true));
        jdbc.update("UPDATE enlaces_acceso SET expira_en=NOW()-INTERVAL '1 minute' WHERE id=?", generado.id());
        assertThatThrownBy(() -> enlaces.consultarPublico(token(generado.enlace())))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void enlaceRevocadoNoPuedeConsultarseNiConsumirse() {
        var generado = enlaces.invitar("revocar-1", new SolicitudInvitacion(UUID.randomUUID(), UUID.randomUUID(),
                "Familia Revocada", "revocado@example.com"), jwt(true));
        String token = token(generado.enlace());

        enlaces.revocar(generado.id(), jwt(true));

        assertThatThrownBy(() -> enlaces.consultarPublico(token))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> enlaces.consumir(new SolicitudConsumirEnlace(token, "ClaveRevocada2026!")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_acceso WHERE entidad_publica_id=? AND operacion='REVOCAR'",
                Integer.class, generado.id())).isEqualTo(1);
    }

    @Test
    void soloUnaSolicitudConcurrenteConsumeLaInvitacion() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        var generado = enlaces.invitar("concurrente-1", new SolicitudInvitacion(usuarioId, UUID.randomUUID(),
                "Familia Concurrente", "concurrente@example.com"), jwt(true));
        String token = token(generado.enlace());
        try (var ejecutor = Executors.newFixedThreadPool(2)) {
            java.util.List<Callable<Boolean>> tareas = java.util.List.of(
                    () -> consumirSeguro(token), () -> consumirSeguro(token));
            var resultados = ejecutor.invokeAll(tareas, 30, TimeUnit.SECONDS);
            assertThat(resultados).allMatch(resultado -> !resultado.isCancelled());
            int exitos = 0;
            for (var resultado : resultados) if (resultado.get()) exitos++;
            assertThat(exitos).isEqualTo(1);
        }
        assertThat(usuarios.findByIdPublico(usuarioId)).isPresent();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM enlaces_acceso WHERE id=? AND consumido_en IS NOT NULL",
                Integer.class, generado.id())).isEqualTo(1);
    }

    private boolean consumirSeguro(String token) {
        try {
            enlaces.consumir(new SolicitudConsumirEnlace(token, "ClaveConcurrente2026!"));
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private String token(String enlace) { return enlace.substring(enlace.indexOf("#token=") + 7); }

    private Jwt jwt(boolean administrador) {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600), Map.of("alg", "none"),
                Map.of("sub", UUID.randomUUID().toString(), "rol_plataforma",
                        administrador ? "ADMINISTRADOR_PLATAFORMA" : "USUARIO"));
    }
}
