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
import com.obusystem.agendafamiliar.agenda.administracion.SolicitudActualizacionMiembroPlataforma;
import com.obusystem.agendafamiliar.agenda.administracion.SolicitudFamiliaPlataforma;
import com.obusystem.agendafamiliar.agenda.administracion.SolicitudMiembroPlataforma;

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
        var administrador = jwt(true);
        var primera = administracion.crear("familia-rivera-1", solicitud, administrador);
        var repetida = administracion.crear("familia-rivera-1", solicitud, administrador);

        assertThat(repetida.id()).isEqualTo(primera.id());
        assertThat(administracion.consultar(administrador).familias()).extracting("id").contains(primera.id());
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

    @Test
    void creaMiembroAdultoIdempotenteYAisladoPorFamilia() {
        Jwt administrador = jwt(true);
        var familia = administracion.crear("familia-miembros-1",
                new SolicitudFamiliaPlataforma("Familia Miembros", "America/Lima"), administrador);
        UUID usuarioId = UUID.randomUUID();
        var solicitud = new SolicitudMiembroPlataforma(usuarioId, "Ana Rivera", "ADMINISTRADOR_FAMILIAR");
        var primero = administracion.crearMiembro(familia.id(), "miembro-ana-1", solicitud, administrador);
        var repetido = administracion.crearMiembro(familia.id(), "miembro-ana-1", solicitud, administrador);

        assertThat(repetido.perfilId()).isEqualTo(primero.perfilId());
        assertThat(administracion.consultarMiembros(familia.id(), administrador).miembros())
                .extracting("usuarioId").containsExactly(usuarioId);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_plataforma WHERE entidad='MIEMBRO' AND entidad_publica_id=?",
                Integer.class, primero.perfilId())).isEqualTo(1);
        assertThatThrownBy(() -> administracion.consultarMiembros(familia.id(), jwt(false)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThatThrownBy(() -> administracion.consultarMiembros(UUID.randomUUID(), administrador))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void exigeAdministradorFamiliarComoPrimerMiembro() {
        Jwt administrador = jwt(true);
        var familia = administracion.crear("familia-primer-admin-1",
                new SolicitudFamiliaPlataforma("Familia Primer Admin", "America/Lima"), administrador);

        assertThatThrownBy(() -> administracion.crearMiembro(familia.id(), "miembro-adulto-primero-1",
                new SolicitudMiembroPlataforma(UUID.randomUUID(), "Persona adulta", "ADULTO"), administrador))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void cambiaRolDaDeBajaYConservaUnAdministradorActivo() {
        Jwt administrador = jwt(true);
        var familia = administracion.crear("familia-roles-1",
                new SolicitudFamiliaPlataforma("Familia Roles", "America/Lima"), administrador);
        var primerAdmin = administracion.crearMiembro(familia.id(), "roles-admin-1",
                new SolicitudMiembroPlataforma(UUID.randomUUID(), "Primera administradora", "ADMINISTRADOR_FAMILIAR"),
                administrador);
        var adulto = administracion.crearMiembro(familia.id(), "roles-adulto-1",
                new SolicitudMiembroPlataforma(UUID.randomUUID(), "Segundo adulto", "ADULTO"), administrador);

        assertThatThrownBy(() -> administracion.actualizarMiembro(familia.id(), primerAdmin.perfilId(),
                new SolicitudActualizacionMiembroPlataforma("ADULTO", true), administrador))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        var promovido = administracion.actualizarMiembro(familia.id(), adulto.perfilId(),
                new SolicitudActualizacionMiembroPlataforma("ADMINISTRADOR_FAMILIAR", true), administrador);
        assertThat(promovido.permiso()).isEqualTo("ADMINISTRADOR_FAMILIAR");

        var baja = administracion.actualizarMiembro(familia.id(), primerAdmin.perfilId(),
                new SolicitudActualizacionMiembroPlataforma("ADULTO", false), administrador);
        assertThat(baja.activo()).isFalse();
        assertThat(administracion.consultarMiembros(familia.id(), administrador).miembros())
                .filteredOn(miembro -> miembro.perfilId().equals(primerAdmin.perfilId()))
                .extracting("activo").containsExactly(false);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria_plataforma WHERE entidad='MIEMBRO' AND entidad_publica_id=? AND operacion='ACTUALIZAR'",
                Integer.class, primerAdmin.perfilId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM auditoria WHERE entidad='PERFIL' AND entidad_publica_id=? AND operacion='ACTUALIZAR'",
                Integer.class, primerAdmin.perfilId())).isEqualTo(1);
    }

    @Test
    void aislaLaActualizacionPorFamiliaYExigeAdministradorDePlataforma() {
        Jwt administrador = jwt(true);
        var familiaA = administracion.crear("familia-aislamiento-roles-a",
                new SolicitudFamiliaPlataforma("Familia A", "America/Lima"), administrador);
        var familiaB = administracion.crear("familia-aislamiento-roles-b",
                new SolicitudFamiliaPlataforma("Familia B", "America/Lima"), administrador);
        var miembroA = administracion.crearMiembro(familiaA.id(), "miembro-aislamiento-roles-a",
                new SolicitudMiembroPlataforma(UUID.randomUUID(), "Persona A", "ADMINISTRADOR_FAMILIAR"), administrador);
        var solicitud = new SolicitudActualizacionMiembroPlataforma("ADULTO", true);

        assertThatThrownBy(() -> administracion.actualizarMiembro(familiaB.id(), miembroA.perfilId(), solicitud, administrador))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> administracion.actualizarMiembro(familiaA.id(), miembroA.perfilId(), solicitud, jwt(false)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Jwt jwt(boolean administrador) {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600), Map.of("alg", "none"),
                Map.of("sub", UUID.randomUUID().toString(), "rol_plataforma",
                        administrador ? "ADMINISTRADOR_PLATAFORMA" : "USUARIO"));
    }
}
