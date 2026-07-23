package com.obusystem.agendafamiliar.autenticacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.obusystem.agendafamiliar.autenticacion.sesion.ServicioSesiones;
import com.obusystem.agendafamiliar.autenticacion.sesion.SolicitudInicioSesion;
import com.obusystem.agendafamiliar.autenticacion.usuario.InicializadorFamiliaTest;
import com.obusystem.agendafamiliar.autenticacion.usuario.RepositorioUsuarios;

@Testcontainers
@SpringBootTest(properties = {
        "familia-test.habilitada=true",
        "familia-test.clave=ClaveTemporalSegura2026!"
})
@TestMethodOrder(OrderAnnotation.class)
class MigracionesAutenticacionIT {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ServicioSesiones sesiones;

    @Autowired
    InicializadorFamiliaTest inicializador;

    @Autowired
    RepositorioUsuarios usuarios;

    @Autowired
    PasswordEncoder claves;

    @Test
    @Order(1)
    void creaLosDosAdultosConCredenciales() {
        Integer cantidad = jdbc.queryForObject(
                "SELECT COUNT(*) FROM usuarios WHERE correo IN ('papa@familia.test', 'mama@familia.test')",
                Integer.class);
        assertThat(cantidad).isEqualTo(2);
        assertThat(usuarios.findByCorreoIgnoreCase("papa@familia.test").orElseThrow().getRolPlataforma())
                .isEqualTo("ADMINISTRADOR_PLATAFORMA");
        Integer tablaSesiones = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'sesiones_refresh'",
                Integer.class);
        assertThat(tablaSesiones).isEqualTo(1);
    }

    @Test
    @Order(2)
    void rotaYRevocaElRefreshTokenSinGuardarElSecreto() {
        var primera = sesiones.iniciar(new SolicitudInicioSesion("papa@familia.test", "ClaveTemporalSegura2026!"));
        assertThat(primera.respuesta().rolPlataforma()).isEqualTo("ADMINISTRADOR_PLATAFORMA");
        var segunda = sesiones.renovar(primera.refreshToken(), primera.csrfToken(), primera.csrfToken());

        Boolean primeraRevocada = jdbc.queryForObject(
                "SELECT revocado_en IS NOT NULL FROM sesiones_refresh WHERE id = ?",
                Boolean.class, primera.sesionId());
        assertThat(primeraRevocada).isTrue();
        assertThat(segunda.refreshToken()).isNotEqualTo(primera.refreshToken());
        assertThatThrownBy(() -> sesiones.renovar(
                primera.refreshToken(), primera.csrfToken(), primera.csrfToken()))
                .isInstanceOf(BadCredentialsException.class);

        sesiones.cerrar(segunda.refreshToken(), segunda.csrfToken(), segunda.csrfToken());
        Boolean segundaRevocada = jdbc.queryForObject(
                "SELECT revocado_en IS NOT NULL FROM sesiones_refresh WHERE id = ?",
                Boolean.class, segunda.sesionId());
        assertThat(segundaRevocada).isTrue();
    }

    @Test
    @Order(3)
    void resincronizaLaClaveConfiguradaEnCuentasExistentes() throws Exception {
        var papa = usuarios.findByCorreoIgnoreCase("papa@familia.test").orElseThrow();
        papa.actualizarClave(claves.encode("ClaveAnteriorQueDebeCambiar2026!"));
        usuarios.saveAndFlush(papa);

        inicializador.run(null);

        var actualizado = usuarios.findByCorreoIgnoreCase("papa@familia.test").orElseThrow();
        assertThat(claves.matches("ClaveTemporalSegura2026!", actualizado.getClaveHash())).isTrue();
    }
}
