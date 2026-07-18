package com.obusystem.agendafamiliar.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.obusystem.agendafamiliar.agenda.archivo.RespuestaArchivo;
import com.obusystem.agendafamiliar.agenda.archivo.ServicioArchivosFamilia;
import com.obusystem.agendafamiliar.agenda.archivo.ProcesadorImagenTest;
import com.obusystem.agendafamiliar.agenda.catalogo.ServicioCatalogo;
import com.obusystem.agendafamiliar.agenda.catalogo.SolicitudesCatalogo;

@Testcontainers
@SpringBootTest
class ArchivosFamiliaIT {
    private static final UUID FAMILIA = UUID.fromString("0197f100-0000-7000-8000-000000000001");
    private static final UUID PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000101");
    private static final UUID PERFIL_PAPA = UUID.fromString("0197f100-0000-7000-8000-000000000201");
    private static final Path RAIZ = Path.of(System.getProperty("java.io.tmpdir"), "agenda-v8-" + UUID.randomUUID());

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry propiedades) {
        propiedades.add("archivos.raiz", () -> RAIZ.toString());
        propiedades.add("archivos.clave", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
    }

    @Autowired ServicioArchivosFamilia archivos;
    @Autowired ServicioCatalogo catalogo;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional
    void guardaCifradoDescargaMiniaturaYNoConservaDatosAnexos() throws Exception {
        UUID tratamiento = tratamiento("Archivo cifrado V8");
        byte[] png = ProcesadorImagenTest.imagenPng(900, 600);
        byte[] marca = "GPS-PRIVADO".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] fuente = java.util.Arrays.copyOf(png, png.length + marca.length);
        System.arraycopy(marca, 0, fuente, png.length, marca.length);

        RespuestaArchivo respuesta = archivos.subirReceta(FAMILIA, tratamiento,
                new MockMultipartFile("archivo", "receta.png", "image/png", fuente), jwt());
        byte[] descargada = archivos.descargar(FAMILIA, respuesta.id(), false, jwt()).contenido();
        byte[] miniatura = archivos.descargar(FAMILIA, respuesta.id(), true, jwt()).contenido();
        String ruta = jdbc.queryForObject("SELECT ruta_original FROM archivos_familia WHERE id_publico=?", String.class, respuesta.id());
        byte[] disco = Files.readAllBytes(RAIZ.resolve(ruta));

        assertThat(descargada).startsWith((byte) 0xff, (byte) 0xd8).doesNotContain(marca);
        assertThat(miniatura.length).isLessThan(descargada.length);
        assertThat(java.util.Arrays.equals(java.util.Arrays.copyOf(disco, 2), new byte[] {(byte) 0xff, (byte) 0xd8})).isFalse();
        assertThat(new String(disco, java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("GPS-PRIVADO");
        assertThat(archivos.cuota(FAMILIA, jwt()).usadosBytes()).isEqualTo(respuesta.bytesAlmacenados());
    }

    @Test
    @Transactional
    void rechazaContenidoInvalidoCuotaEIdor() throws Exception {
        UUID tratamiento = tratamiento("Validaciones V8");
        assertThatThrownBy(() -> archivos.subirReceta(FAMILIA, tratamiento,
                new MockMultipartFile("archivo", "ataque.svg", "image/svg+xml", "<svg/>".getBytes()), jwt()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        Long familiaInterna = jdbc.queryForObject("SELECT id FROM familias WHERE id_publico=?", Long.class, FAMILIA);
        jdbc.update("UPDATE familias SET cuota_bytes=1 WHERE id=?", familiaInterna);
        assertThatThrownBy(() -> archivos.subirReceta(FAMILIA, tratamiento,
                foto("cuota.png", ProcesadorImagenTest.imagenPng(80, 80)), jwt()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(error -> ((ResponseStatusException) error).getStatusCode())
                .isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);

        UUID otraFamilia = jdbc.queryForObject("INSERT INTO familias (id_publico, nombre) VALUES (gen_random_uuid(), 'Archivos ajenos V8') RETURNING id_publico", UUID.class);
        assertThatThrownBy(() -> archivos.subirReceta(otraFamilia, tratamiento,
                foto("idor.png", ProcesadorImagenTest.imagenPng(80, 80)), jwt()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Familia no encontrada");
    }

    @Test
    void dosSubidasCompetidorasNoCreanDosRecetas() throws Exception {
        UUID tratamiento = tratamiento("Concurrencia archivo V8 " + UUID.randomUUID());
        byte[] contenido = ProcesadorImagenTest.imagenPng(120, 90);
        CountDownLatch salida = new CountDownLatch(1);
        try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            var primera = ejecutor.submit(() -> competir(salida, tratamiento, contenido, "a.png"));
            var segunda = ejecutor.submit(() -> competir(salida, tratamiento, contenido, "b.png"));
            salida.countDown();
            assertThat(List.of(primera.get(), segunda.get())).containsExactlyInAnyOrder("OK", "CONFLICT");
        }
        Long familiaInterna = jdbc.queryForObject("SELECT id FROM familias WHERE id_publico=?", Long.class, FAMILIA);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM archivos_familia WHERE familia_id=? AND tratamiento_id=(SELECT id FROM tratamientos WHERE id_publico=?) AND estado='ACTIVO'",
                Integer.class, familiaInterna, tratamiento)).isEqualTo(1);
        var meta = jdbc.queryForMap("SELECT id_publico, ruta_original, ruta_miniatura FROM archivos_familia WHERE familia_id=? AND tratamiento_id=(SELECT id FROM tratamientos WHERE id_publico=?) AND estado='ACTIVO'",
                familiaInterna, tratamiento);
        UUID archivoId = (UUID) meta.get("id_publico");
        Path original = RAIZ.resolve((String) meta.get("ruta_original"));
        Path miniatura = RAIZ.resolve((String) meta.get("ruta_miniatura"));
        assertThat(original).exists();
        assertThat(miniatura).exists();

        archivos.eliminar(FAMILIA, archivoId, jwt());

        assertThat(original).doesNotExist();
        assertThat(miniatura).doesNotExist();
        assertThatThrownBy(() -> archivos.descargar(FAMILIA, archivoId, false, jwt()))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("no encontrado");
    }

    @Test
    void cuotaSerializaSubidasDeTratamientosDistintos() throws Exception {
        UUID primero = tratamiento("Cuota concurrente A " + UUID.randomUUID());
        UUID segundo = tratamiento("Cuota concurrente B " + UUID.randomUUID());
        byte[] fotoA = ProcesadorImagenTest.imagenPng(130, 90);
        byte[] fotoB = ProcesadorImagenTest.imagenPng(131, 90);
        RespuestaArchivo medidaA = archivos.subirReceta(FAMILIA, primero, foto("medida-a.png", fotoA), jwt());
        archivos.eliminar(FAMILIA, medidaA.id(), jwt());
        RespuestaArchivo medidaB = archivos.subirReceta(FAMILIA, segundo, foto("medida-b.png", fotoB), jwt());
        archivos.eliminar(FAMILIA, medidaB.id(), jwt());
        Long familiaInterna = jdbc.queryForObject("SELECT id FROM familias WHERE id_publico=?", Long.class, FAMILIA);
        long cuotaUna = Math.max(medidaA.bytesAlmacenados(), medidaB.bytesAlmacenados()) + 16;
        jdbc.update("UPDATE familias SET cuota_bytes=? WHERE id=?", cuotaUna, familiaInterna);
        try {
            CountDownLatch salida = new CountDownLatch(1);
            try (var ejecutor = Executors.newVirtualThreadPerTaskExecutor()) {
                var subidaA = ejecutor.submit(() -> competirCuota(salida, primero, fotoA, "cuota-a.png"));
                var subidaB = ejecutor.submit(() -> competirCuota(salida, segundo, fotoB, "cuota-b.png"));
                salida.countDown();
                assertThat(List.of(subidaA.get(), subidaB.get())).containsExactlyInAnyOrder("OK", "CUOTA");
            }
            assertThat(archivos.cuota(FAMILIA, jwt()).usadosBytes()).isLessThanOrEqualTo(cuotaUna).isPositive();
        } finally {
            jdbc.update("UPDATE familias SET cuota_bytes=1073741824 WHERE id=?", familiaInterna);
        }
    }

    private String competir(CountDownLatch salida, UUID tratamiento, byte[] contenido, String nombre) throws Exception {
        salida.await();
        try {
            archivos.subirReceta(FAMILIA, tratamiento, foto(nombre, contenido), jwt());
            return "OK";
        } catch (ResponseStatusException error) {
            return error.getStatusCode().equals(HttpStatus.CONFLICT) ? "CONFLICT" : error.getStatusCode().toString();
        } catch (org.springframework.dao.DataIntegrityViolationException error) {
            return "CONFLICT";
        }
    }

    private String competirCuota(CountDownLatch salida, UUID tratamiento, byte[] contenido, String nombre) throws Exception {
        salida.await();
        try {
            archivos.subirReceta(FAMILIA, tratamiento, foto(nombre, contenido), jwt());
            return "OK";
        } catch (ResponseStatusException error) {
            return error.getStatusCode().equals(HttpStatus.PAYLOAD_TOO_LARGE) ? "CUOTA" : error.getStatusCode().toString();
        }
    }

    private UUID tratamiento(String nombre) {
        return catalogo.crearTratamiento(FAMILIA, new SolicitudesCatalogo.Tratamiento(PERFIL_PAPA, null, nombre,
                null, null, null, LocalTime.of(9, 0), null, null, LocalDate.now(),
                LocalDate.now().plusDays(1), PERFIL_PAPA, null), jwt());
    }

    private MockMultipartFile foto(String nombre, byte[] contenido) {
        return new MockMultipartFile("archivo", nombre, "image/png", contenido);
    }

    private Jwt jwt() {
        return new Jwt("prueba", Instant.now(), Instant.now().plusSeconds(600),
                Map.of("alg", "none"), Map.of("sub", PAPA.toString()));
    }
}
