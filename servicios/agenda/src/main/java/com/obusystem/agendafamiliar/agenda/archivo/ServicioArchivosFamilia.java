package com.obusystem.agendafamiliar.agenda.archivo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.obusystem.agendafamiliar.agenda.family.AccesoFamilia;
import com.obusystem.agendafamiliar.agenda.family.Familia;
import com.obusystem.agendafamiliar.agenda.util.UuidV7;

@Service
public class ServicioArchivosFamilia {
    private final AccesoFamilia acceso;
    private final JdbcTemplate jdbc;
    private final ProcesadorImagen imagenes;
    private final AlmacenCifrado almacen;

    public ServicioArchivosFamilia(AccesoFamilia acceso, JdbcTemplate jdbc,
            ProcesadorImagen imagenes, AlmacenCifrado almacen) {
        this.acceso = acceso;
        this.jdbc = jdbc;
        this.imagenes = imagenes;
        this.almacen = almacen;
    }

    @Transactional
    public RespuestaArchivo subirReceta(UUID familiaId, UUID tratamientoId, MultipartFile archivo, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        Long tratamientoInterno = tratamiento(familia.getId(), tratamientoId);
        if (existeReceta(familia.getId(), tratamientoInterno)) {
            throw problema(HttpStatus.CONFLICT, "El tratamiento ya tiene una fotografía de receta");
        }
        ImagenProcesada procesada;
        try {
            procesada = imagenes.procesar(archivo.getBytes(), archivo.getContentType());
        } catch (java.io.IOException error) {
            throw problema(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen recibida");
        }
        long cuota = jdbc.queryForObject("SELECT cuota_bytes FROM familias WHERE id=? FOR UPDATE", Long.class, familia.getId());
        if (existeReceta(familia.getId(), tratamientoInterno)) {
            throw problema(HttpStatus.CONFLICT, "El tratamiento ya tiene una fotografía de receta");
        }
        if (jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM archivos_familia WHERE familia_id=? AND sha256=? AND estado='ACTIVO')",
                Boolean.class, familia.getId(), procesada.sha256())) {
            throw problema(HttpStatus.CONFLICT, "La misma imagen ya está guardada en esta familia");
        }
        long usados = usados(familia.getId());
        UUID id = UuidV7.nuevo();
        String directorio = familia.getIdPublico() + "/" + id;
        String rutaOriginal = directorio + "-original.bin";
        String rutaMiniatura = directorio + "-miniatura.bin";
        long bytesOriginal = 0;
        try {
            bytesOriginal = almacen.guardar(rutaOriginal, procesada.original());
            long bytesMiniatura = almacen.guardar(rutaMiniatura, procesada.miniatura());
            long bytesAlmacenados = bytesOriginal + bytesMiniatura;
            if (usados + bytesAlmacenados > cuota) {
                throw problema(HttpStatus.PAYLOAD_TOO_LARGE, "La familia alcanzó su cuota de almacenamiento");
            }
            registrarLimpiezaSiRevierte(rutaOriginal, rutaMiniatura);
            try {
                jdbc.update("INSERT INTO archivos_familia (id_publico, familia_id, tratamiento_id, mime_type, ancho, alto, bytes_fuente, bytes_almacenados, sha256, ruta_original, ruta_miniatura) VALUES (?, ?, ?, 'image/jpeg', ?, ?, ?, ?, ?, ?, ?)",
                        id, familia.getId(), tratamientoInterno, procesada.ancho(), procesada.alto(), archivo.getSize(),
                        bytesAlmacenados, procesada.sha256(), rutaOriginal, rutaMiniatura);
            } catch (org.springframework.dao.DataIntegrityViolationException error) {
                throw problema(HttpStatus.CONFLICT, "La receta ya fue guardada por otra solicitud");
            }
            auditar(familia.getId(), jwt, "SUBIR", id, "Fotografía de receta guardada de forma privada");
            return new RespuestaArchivo(id, tratamientoId, procesada.ancho(), procesada.alto(), bytesAlmacenados, Instant.now());
        } catch (RuntimeException error) {
            almacen.eliminar(rutaOriginal);
            almacen.eliminar(rutaMiniatura);
            throw error;
        }
    }

    @Transactional(readOnly = true)
    public ContenidoArchivo descargar(UUID familiaId, UUID archivoId, boolean miniatura, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        MetaArchivo meta = meta(familia.getId(), archivoId);
        return new ContenidoArchivo(meta.idPublico(), "image/jpeg",
                almacen.leer(miniatura ? meta.rutaMiniatura() : meta.rutaOriginal()));
    }

    @Transactional
    public void eliminar(UUID familiaId, UUID archivoId, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        MetaArchivo meta = meta(familia.getId(), archivoId);
        int cambio = jdbc.update("UPDATE archivos_familia SET estado='BORRADO', eliminado_en=NOW(), actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id_publico=? AND estado='ACTIVO'",
                familia.getId(), archivoId);
        if (cambio == 0) throw problema(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        auditar(familia.getId(), jwt, "ELIMINAR", archivoId, "Fotografía de receta eliminada");
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                almacen.eliminar(meta.rutaOriginal());
                almacen.eliminar(meta.rutaMiniatura());
            }
        });
    }

    @Transactional(readOnly = true)
    public RespuestaCuota cuota(UUID familiaId, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        long cuota = jdbc.queryForObject("SELECT cuota_bytes FROM familias WHERE id=?", Long.class, familia.getId());
        return cuota(cuota, usados(familia.getId()));
    }

    private Long tratamiento(Long familiaId, UUID tratamientoId) {
        List<Long> ids = jdbc.query("SELECT id FROM tratamientos WHERE familia_id=? AND id_publico=?",
                (rs, fila) -> rs.getLong(1), familiaId, tratamientoId);
        if (ids.isEmpty()) throw problema(HttpStatus.NOT_FOUND, "Tratamiento no encontrado");
        return ids.getFirst();
    }

    private boolean existeReceta(Long familiaId, Long tratamientoId) {
        return jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM archivos_familia WHERE familia_id=? AND tratamiento_id=? AND estado='ACTIVO')",
                Boolean.class, familiaId, tratamientoId);
    }

    private long usados(Long familiaId) {
        Long valor = jdbc.queryForObject("SELECT COALESCE(SUM(bytes_almacenados), 0) FROM archivos_familia WHERE familia_id=? AND estado='ACTIVO'",
                Long.class, familiaId);
        return valor == null ? 0 : valor;
    }

    private RespuestaCuota cuota(long cuota, long usados) {
        int porcentaje = cuota == 0 ? 100 : (int) Math.min(100, Math.ceil(usados * 100d / cuota));
        String nivel = porcentaje >= 100 ? "BLOQUEADA" : porcentaje >= 95 ? "CRITICA"
                : porcentaje >= 85 ? "ALTA" : porcentaje >= 70 ? "MEDIA" : "NORMAL";
        return new RespuestaCuota(cuota, usados, Math.max(0, cuota - usados), porcentaje, nivel);
    }

    private MetaArchivo meta(Long familiaId, UUID archivoId) {
        List<MetaArchivo> filas = jdbc.query("SELECT id_publico, ruta_original, ruta_miniatura FROM archivos_familia WHERE familia_id=? AND id_publico=? AND estado='ACTIVO'",
                (rs, fila) -> new MetaArchivo(rs.getObject("id_publico", UUID.class), rs.getString("ruta_original"),
                        rs.getString("ruta_miniatura")), familiaId, archivoId);
        if (filas.isEmpty()) throw problema(HttpStatus.NOT_FOUND, "Archivo no encontrado");
        return filas.getFirst();
    }

    private void registrarLimpiezaSiRevierte(String original, String miniatura) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int estado) {
                if (estado != STATUS_COMMITTED) {
                    almacen.eliminar(original);
                    almacen.eliminar(miniatura);
                }
            }
        });
    }

    private void auditar(Long familiaId, Jwt jwt, String operacion, UUID id, String resumen) {
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, ?, 'ARCHIVO_RECETA', ?, ?)",
                familiaId, UUID.fromString(jwt.getSubject()), operacion, id, resumen);
    }

    private ResponseStatusException problema(HttpStatus estado, String mensaje) {
        return new ResponseStatusException(estado, mensaje);
    }

    private record MetaArchivo(UUID idPublico, String rutaOriginal, String rutaMiniatura) { }
}
