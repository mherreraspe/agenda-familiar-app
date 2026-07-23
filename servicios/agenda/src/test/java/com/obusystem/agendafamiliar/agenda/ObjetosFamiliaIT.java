package com.obusystem.agendafamiliar.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

import com.obusystem.agendafamiliar.agenda.auditoria.ServicioAuditoria;
import com.obusystem.agendafamiliar.agenda.objetos.ServicioObjetos;
import com.obusystem.agendafamiliar.agenda.objetos.SolicitudObjeto;

@Testcontainers
@SpringBootTest
class ObjetosFamiliaIT {
    private static final UUID FAMILIA = UUID.fromString("0197f100-0000-7000-8000-000000000001");
    private static final UUID PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000101");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @Autowired ServicioObjetos objetos;
    @Autowired ServicioAuditoria auditoria;
    @Autowired JdbcTemplate jdbc;

    @Test
    void persisteBuscaPorNombreCategoriaYLugarYAuditaCambios() {
        UUID id = objetos.crear(FAMILIA, "crear-pasaporte", solicitud("Pasaporte de Lucía", "Documentos",
                List.of("Habitación principal", "Ropero", "Caja de documentos")), jwt());
        assertThat(objetos.crear(FAMILIA, "crear-pasaporte", solicitud("Pasaporte duplicado", "Documentos",
                List.of("Otro lugar")), jwt())).isEqualTo(id);

        assertThat(objetos.consultar(FAMILIA, "lucia", jwt()).objetos()).singleElement()
                .satisfies(objeto -> {
                    assertThat(objeto.id()).isEqualTo(id);
                    assertThat(objeto.ruta()).containsExactly("Habitación principal", "Ropero", "Caja de documentos");
                });
        assertThat(objetos.consultar(FAMILIA, "documentos", jwt()).objetos()).extracting("id").contains(id);
        assertThat(objetos.consultar(FAMILIA, "habitacion", jwt()).objetos()).extracting("id").contains(id);

        objetos.actualizar(FAMILIA, id, solicitud("Pasaporte de Lucía", "Documentos importantes",
                List.of("Estudio", "Archivador")), jwt());
        assertThat(objetos.consultar(FAMILIA, "archivador", jwt()).objetos()).singleElement()
                .satisfies(objeto -> assertThat(objeto.version()).isEqualTo(1));
        assertThat(auditoria.consultar(FAMILIA, jwt()).entradas().stream().filter(entrada -> entrada.entidadId().equals(id)))
                .extracting("operacion").contains("CREAR", "ACTUALIZAR");
        assertThatThrownBy(() -> objetos.actualizar(FAMILIA, id,
                solicitud("Cambio antiguo", "Documentos", List.of("Otro lugar")), jwt()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void ocultaFamiliasYObjetosAjenosComoNoEncontrados() {
        UUID familiaAjena = jdbc.queryForObject(
                "INSERT INTO familias (id_publico, nombre) VALUES (gen_random_uuid(), 'Familia ajena objetos') RETURNING id_publico",
                UUID.class);
        assertThatThrownBy(() -> objetos.consultar(familiaAjena, "", jwt()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> objetos.actualizar(FAMILIA, UUID.randomUUID(),
                solicitud("Objeto", "Categoría", List.of("Lugar")), jwt()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void fuerzaRlsYReutilizaLaMismaRutaBajoConcurrencia() {
        assertThat(jdbc.queryForMap("SELECT relrowsecurity, relforcerowsecurity FROM pg_class WHERE relname='objetos_familia'").values())
                .containsOnly(true);
        assertThat(jdbc.queryForMap("SELECT relrowsecurity, relforcerowsecurity FROM pg_class WHERE relname='ubicaciones_objetos'").values())
                .containsOnly(true);

        var primero = CompletableFuture.runAsync(() -> objetos.crear(FAMILIA, "crear-llave-uno",
                solicitud("Llave uno", "Llaves", List.of("Entrada", "Cajón")), jwt()));
        var segundo = CompletableFuture.runAsync(() -> objetos.crear(FAMILIA, "crear-llave-dos",
                solicitud("Llave dos", "Llaves", List.of("Entrada", "Cajón")), jwt()));
        CompletableFuture.allOf(primero, segundo).join();

        assertThat(objetos.consultar(FAMILIA, "entrada", jwt()).objetos()).extracting("nombre")
                .contains("Llave uno", "Llave dos");
        Long familiaInterna = jdbc.queryForObject("SELECT id FROM familias WHERE id_publico=?", Long.class, FAMILIA);
        jdbc.queryForObject("SELECT set_config('agenda.familia_id', ?, false)", String.class, familiaInterna.toString());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ubicaciones_objetos WHERE familia_id=? AND nombre_normalizado='cajon'", Integer.class, familiaInterna))
                .isEqualTo(1);
    }

    private SolicitudObjeto solicitud(String nombre, String categoria, List<String> ruta) {
        return new SolicitudObjeto(nombre, categoria, "Nota reconocible", 0L, ruta);
    }

    private Jwt jwt() {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), Map.of("sub", PAPA.toString()));
    }
}
