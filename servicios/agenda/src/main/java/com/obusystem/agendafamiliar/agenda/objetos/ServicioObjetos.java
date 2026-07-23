package com.obusystem.agendafamiliar.agenda.objetos;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.obusystem.agendafamiliar.agenda.family.AccesoFamilia;
import com.obusystem.agendafamiliar.agenda.family.Familia;
import com.obusystem.agendafamiliar.agenda.util.UuidV7;

@Service
public class ServicioObjetos {
    private final AccesoFamilia acceso;
    private final JdbcTemplate jdbc;

    public ServicioObjetos(AccesoFamilia acceso, JdbcTemplate jdbc) {
        this.acceso = acceso;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public RespuestaObjetos consultar(UUID familiaId, String consulta, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        String termino = normalizar(consulta);
        String filtro = termino.isEmpty() ? "%" : "%" + termino + "%";
        var objetos = jdbc.query("WITH RECURSIVE rutas AS ("
                + "SELECT u.id, u.familia_id, u.padre_id, ARRAY[u.nombre]::VARCHAR[] ruta FROM ubicaciones_objetos u WHERE u.familia_id=? AND u.padre_id IS NULL "
                + "UNION ALL SELECT h.id, h.familia_id, h.padre_id, r.ruta || h.nombre FROM ubicaciones_objetos h JOIN rutas r ON r.id=h.padre_id AND r.familia_id=h.familia_id) "
                + "SELECT o.id_publico, o.nombre, o.categoria, o.notas, r.ruta, o.actualizado_en, o.version FROM objetos_familia o JOIN rutas r ON r.id=o.ubicacion_id "
                + "WHERE o.familia_id=? AND (o.nombre_normalizado LIKE ? OR o.categoria_normalizada LIKE ? OR EXISTS (SELECT 1 FROM unnest(r.ruta) segmento WHERE lower(translate(segmento, 'ÁÉÍÓÚÜÑáéíóúüñ', 'AEIOUUNaeiouun')) LIKE ?)) "
                + "ORDER BY o.actualizado_en DESC, o.nombre LIMIT 100",
                (rs, fila) -> new RespuestaObjetos.Objeto(rs.getObject("id_publico", UUID.class), rs.getString("nombre"),
                        rs.getString("categoria"), rs.getString("notas"), List.of((String[]) rs.getArray("ruta").getArray()),
                        rs.getTimestamp("actualizado_en").toInstant(), rs.getLong("version")),
                familia.getId(), familia.getId(), filtro, filtro, filtro);
        var ubicaciones = jdbc.query("WITH RECURSIVE rutas AS ("
                + "SELECT u.id, u.familia_id, ARRAY[u.nombre]::VARCHAR[] ruta FROM ubicaciones_objetos u WHERE u.familia_id=? AND u.padre_id IS NULL "
                + "UNION ALL SELECT h.id, h.familia_id, r.ruta || h.nombre FROM ubicaciones_objetos h JOIN rutas r ON r.id=h.padre_id AND r.familia_id=h.familia_id) "
                + "SELECT r.ruta, COUNT(o.id)::INTEGER cantidad FROM rutas r JOIN objetos_familia o ON o.ubicacion_id=r.id AND o.familia_id=r.familia_id "
                + "GROUP BY r.ruta ORDER BY cantidad DESC, r.ruta LIMIT 30",
                (rs, fila) -> new RespuestaObjetos.Ubicacion(List.of((String[]) rs.getArray("ruta").getArray()), rs.getInt("cantidad")),
                familia.getId());
        return new RespuestaObjetos(objetos, ubicaciones);
    }

    @Transactional
    public UUID crear(UUID familiaId, String claveIdempotencia, SolicitudObjeto solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        if (claveIdempotencia == null || claveIdempotencia.isBlank() || claveIdempotencia.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key inválida");
        }
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", (rs, fila) -> 0,
                familia.getId() + ":" + claveIdempotencia);
        List<UUID> anterior = jdbc.query("SELECT objeto_publico_id FROM idempotencia_objetos WHERE familia_id=? AND clave=?",
                (rs, fila) -> rs.getObject(1, UUID.class), familia.getId(), claveIdempotencia);
        if (!anterior.isEmpty()) return anterior.getFirst();
        Long ubicacion = asegurarRuta(familia.getId(), solicitud.ruta());
        UUID id = UuidV7.nuevo();
        jdbc.update("INSERT INTO objetos_familia (id_publico, familia_id, ubicacion_id, nombre, nombre_normalizado, categoria, categoria_normalizada, notas) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, familia.getId(), ubicacion, solicitud.nombre().trim(), normalizar(solicitud.nombre()),
                solicitud.categoria().trim(), normalizar(solicitud.categoria()), limpiar(solicitud.notas()));
        jdbc.update("INSERT INTO idempotencia_objetos (familia_id, clave, objeto_publico_id) VALUES (?, ?, ?)",
                familia.getId(), claveIdempotencia, id);
        auditar(familia.getId(), jwt, "CREAR", id, "Objeto familiar registrado");
        return id;
    }

    @Transactional
    public void actualizar(UUID familiaId, UUID objetoId, SolicitudObjeto solicitud, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        if (solicitud.version() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La versión del objeto es obligatoria al editar");
        }
        Long ubicacion = asegurarRuta(familia.getId(), solicitud.ruta());
        int filas = jdbc.update("UPDATE objetos_familia SET ubicacion_id=?, nombre=?, nombre_normalizado=?, categoria=?, categoria_normalizada=?, notas=?, actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id_publico=? AND version=?",
                ubicacion, solicitud.nombre().trim(), normalizar(solicitud.nombre()), solicitud.categoria().trim(),
                normalizar(solicitud.categoria()), limpiar(solicitud.notas()), familia.getId(), objetoId, solicitud.version());
        if (filas == 0) {
            Integer existe = jdbc.queryForObject("SELECT COUNT(*) FROM objetos_familia WHERE familia_id=? AND id_publico=?", Integer.class,
                    familia.getId(), objetoId);
            if (existe == null || existe == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Objeto no encontrado");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El objeto cambió en otro dispositivo. Recarga antes de editarlo.");
        }
        auditar(familia.getId(), jwt, "ACTUALIZAR", objetoId, "Objeto familiar actualizado");
    }

    private Long asegurarRuta(Long familiaId, List<String> rutaSolicitada) {
        List<String> ruta = rutaSolicitada.stream().map(String::trim).filter(valor -> !valor.isEmpty()).toList();
        if (ruta.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ubicación debe tener al menos un nivel");
        Long padre = null;
        for (String nombre : ruta) {
            padre = jdbc.queryForObject("INSERT INTO ubicaciones_objetos (id_publico, familia_id, padre_id, nombre, nombre_normalizado) VALUES (?, ?, ?, ?, ?) "
                    + "ON CONFLICT (familia_id, padre_id, nombre_normalizado) DO UPDATE SET nombre=EXCLUDED.nombre RETURNING id",
                    Long.class, UuidV7.nuevo(), familiaId, padre, nombre, normalizar(nombre));
        }
        return padre;
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        String descompuesto = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD);
        return descompuesto.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void auditar(Long familiaId, Jwt jwt, String operacion, UUID entidadId, String resumen) {
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, ?, 'OBJETO', ?, ?)",
                familiaId, UUID.fromString(jwt.getSubject()), operacion, entidadId, resumen);
    }
}
