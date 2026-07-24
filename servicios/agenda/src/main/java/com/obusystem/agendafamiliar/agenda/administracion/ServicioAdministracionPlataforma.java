package com.obusystem.agendafamiliar.agenda.administracion;

import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.obusystem.agendafamiliar.agenda.util.UuidV7;
import com.obusystem.agendafamiliar.agenda.family.ContextoFamilia;

@Service
public class ServicioAdministracionPlataforma {
    private static final String ROL_ADMIN = "ADMINISTRADOR_PLATAFORMA";
    private final JdbcTemplate jdbc;
    private final ContextoFamilia contexto;

    public ServicioAdministracionPlataforma(JdbcTemplate jdbc, ContextoFamilia contexto) {
        this.jdbc = jdbc;
        this.contexto = contexto;
    }

    @Transactional(readOnly = true)
    public RespuestaFamiliasPlataforma consultar(Jwt jwt) {
        autorizar(jwt);
        var familias = jdbc.query("SELECT id_publico, nombre, zona_horaria, creado_en FROM familias ORDER BY creado_en DESC, nombre LIMIT 200",
                (rs, fila) -> new RespuestaFamiliasPlataforma.FamiliaAdministrada(
                        rs.getObject("id_publico", UUID.class), rs.getString("nombre"),
                        rs.getString("zona_horaria"), rs.getTimestamp("creado_en").toInstant()));
        return new RespuestaFamiliasPlataforma(familias);
    }

    @Transactional
    public RespuestaFamiliasPlataforma.FamiliaAdministrada crear(String clave, SolicitudFamiliaPlataforma solicitud, Jwt jwt) {
        UUID actor = autorizar(jwt);
        if (clave == null || clave.isBlank() || clave.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key inválida");
        }
        String zona = validarZona(solicitud.zonaHoraria());
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", (rs, fila) -> 0,
                actor + ":" + clave);
        List<UUID> anterior = jdbc.query("SELECT familia_publica_id FROM idempotencia_familias WHERE actor_publico_id=? AND clave=?",
                (rs, fila) -> rs.getObject(1, UUID.class), actor, clave);
        if (!anterior.isEmpty()) return buscar(anterior.getFirst());

        UUID id = UuidV7.nuevo();
        jdbc.update("INSERT INTO familias (id_publico, nombre, zona_horaria) VALUES (?, ?, ?)",
                id, solicitud.nombre().trim(), zona);
        jdbc.update("INSERT INTO idempotencia_familias (clave, familia_publica_id, actor_publico_id) VALUES (?, ?, ?)",
                clave, id, actor);
        jdbc.update("INSERT INTO auditoria_plataforma (actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, 'CREAR', 'FAMILIA', ?, 'Familia registrada desde administración')",
                actor, id);
        return buscar(id);
    }

    @Transactional(readOnly = true)
    public RespuestaMiembrosPlataforma consultarMiembros(UUID familiaId, Jwt jwt) {
        autorizar(jwt);
        Long familiaInterna = familiaInterna(familiaId);
        contexto.activar(familiaInterna);
        return new RespuestaMiembrosPlataforma(jdbc.query("""
                SELECT p.id_publico, p.usuario_publico_id, p.nombre_visible, m.rol, p.activo
                FROM perfiles p
                JOIN miembros_familia m ON m.familia_id=p.familia_id AND m.usuario_publico_id=p.usuario_publico_id
                WHERE p.familia_id=?
                ORDER BY p.activo DESC, p.nombre_visible
                """, (rs, fila) -> new RespuestaMiembrosPlataforma.MiembroAdministrado(
                        rs.getObject("id_publico", UUID.class), rs.getObject("usuario_publico_id", UUID.class),
                        rs.getString("nombre_visible"), rs.getString("rol"), rs.getBoolean("activo")), familiaInterna));
    }

    @Transactional
    public RespuestaMiembrosPlataforma.MiembroAdministrado crearMiembro(UUID familiaId, String clave,
            SolicitudMiembroPlataforma solicitud, Jwt jwt) {
        UUID actor = autorizar(jwt);
        validarClave(clave);
        String permiso = validarPermiso(solicitud.permiso());
        Long familiaInterna = familiaInterna(familiaId);
        jdbc.query("SELECT pg_advisory_xact_lock(hashtextextended(?, 0))", (rs, fila) -> 0, actor + ":" + clave);
        List<UUID> anterior = jdbc.query("SELECT perfil_publico_id FROM idempotencia_miembros_plataforma WHERE actor_publico_id=? AND clave=?",
                (rs, fila) -> rs.getObject(1, UUID.class), actor, clave);
        contexto.activar(familiaInterna);
        if (!anterior.isEmpty()) return buscarMiembro(familiaInterna, anterior.getFirst());
        Boolean existe = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM perfiles WHERE familia_id=? AND usuario_publico_id=?)",
                Boolean.class, familiaInterna, solicitud.usuarioId());
        if (Boolean.TRUE.equals(existe)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta ya está vinculada a esta familia");
        }
        Integer miembrosActivos = jdbc.queryForObject("SELECT COUNT(*) FROM miembros_familia WHERE familia_id=? AND activo",
                Integer.class, familiaInterna);
        if (miembrosActivos == 0 && !"ADMINISTRADOR_FAMILIAR".equals(permiso)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El primer miembro debe administrar la familia");
        }
        UUID perfilId = UuidV7.nuevo();
        jdbc.update("INSERT INTO perfiles (id_publico, familia_id, nombre_visible, tipo, color, relacion, usuario_publico_id, activo) VALUES (?, ?, ?, 'ADULTO', '#315b4c', NULL, ?, TRUE)",
                perfilId, familiaInterna, solicitud.nombre().trim(), solicitud.usuarioId());
        Long perfilInterno = jdbc.queryForObject("SELECT id FROM perfiles WHERE familia_id=? AND id_publico=?",
                Long.class, familiaInterna, perfilId);
        jdbc.update("INSERT INTO miembros_familia (familia_id, usuario_publico_id, rol, activo, perfil_id) VALUES (?, ?, ?, TRUE, ?)",
                familiaInterna, solicitud.usuarioId(), permiso, perfilInterno);
        jdbc.update("INSERT INTO idempotencia_miembros_plataforma (actor_publico_id, clave, familia_publica_id, perfil_publico_id, usuario_publico_id) VALUES (?, ?, ?, ?, ?)",
                actor, clave, familiaId, perfilId, solicitud.usuarioId());
        jdbc.update("INSERT INTO auditoria_plataforma (actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, 'CREAR', 'MIEMBRO', ?, 'Miembro con acceso registrado')",
                actor, perfilId);
        return buscarMiembro(familiaInterna, perfilId);
    }

    @Transactional
    public RespuestaMiembrosPlataforma.MiembroAdministrado actualizarMiembro(UUID familiaId, UUID perfilId,
            SolicitudActualizacionMiembroPlataforma solicitud, Jwt jwt) {
        UUID actor = autorizar(jwt);
        String permiso = validarPermiso(solicitud.permiso());
        Long familiaInterna = familiaInterna(familiaId);
        contexto.activar(familiaInterna);
        jdbc.queryForList("SELECT pg_advisory_xact_lock(?)", familiaInterna);
        MiembroActual actual = miembroActual(familiaInterna, perfilId);
        protegerUltimoAdministrador(familiaInterna, actual, permiso, solicitud.activo());

        int perfiles = jdbc.update("UPDATE perfiles SET activo=?, actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id_publico=?",
                solicitud.activo(), familiaInterna, perfilId);
        int miembros = jdbc.update("UPDATE miembros_familia SET rol=?, activo=?, actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND usuario_publico_id=? AND perfil_id=?",
                permiso, solicitud.activo(), familiaInterna, actual.usuarioId(), actual.perfilInternoId());
        if (perfiles != 1 || miembros != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro no encontrado en esta familia");
        }
        String resumen = solicitud.activo()
                ? "Rol o acceso familiar actualizado desde administración"
                : "Acceso a la familia dado de baja desde administración";
        jdbc.update("INSERT INTO auditoria_plataforma (actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, 'ACTUALIZAR', 'MIEMBRO', ?, ?)",
                actor, perfilId, resumen);
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, 'ACTUALIZAR', 'PERFIL', ?, ?)",
                familiaInterna, actor, perfilId, resumen);
        return buscarMiembro(familiaInterna, perfilId);
    }

    private RespuestaFamiliasPlataforma.FamiliaAdministrada buscar(UUID id) {
        return jdbc.queryForObject("SELECT id_publico, nombre, zona_horaria, creado_en FROM familias WHERE id_publico=?",
                (rs, fila) -> new RespuestaFamiliasPlataforma.FamiliaAdministrada(
                        rs.getObject("id_publico", UUID.class), rs.getString("nombre"),
                        rs.getString("zona_horaria"), rs.getTimestamp("creado_en").toInstant()), id);
    }

    private RespuestaMiembrosPlataforma.MiembroAdministrado buscarMiembro(Long familiaInterna, UUID perfilId) {
        return jdbc.queryForObject("""
                SELECT p.id_publico, p.usuario_publico_id, p.nombre_visible, m.rol, p.activo
                FROM perfiles p
                JOIN miembros_familia m ON m.familia_id=p.familia_id AND m.usuario_publico_id=p.usuario_publico_id
                WHERE p.familia_id=? AND p.id_publico=?
                """, (rs, fila) -> new RespuestaMiembrosPlataforma.MiembroAdministrado(
                        rs.getObject("id_publico", UUID.class), rs.getObject("usuario_publico_id", UUID.class),
                        rs.getString("nombre_visible"), rs.getString("rol"), rs.getBoolean("activo")), familiaInterna, perfilId);
    }

    private MiembroActual miembroActual(Long familiaInterna, UUID perfilId) {
        List<MiembroActual> filas = jdbc.query("""
                SELECT m.id, m.usuario_publico_id, m.rol, m.activo
                FROM perfiles p
                JOIN miembros_familia m ON m.familia_id=p.familia_id AND m.usuario_publico_id=p.usuario_publico_id
                WHERE p.familia_id=? AND p.id_publico=?
                """, (rs, fila) -> new MiembroActual(rs.getLong("id"),
                        rs.getObject("usuario_publico_id", UUID.class), rs.getString("rol"),
                        rs.getBoolean("activo")), familiaInterna, perfilId);
        if (filas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Miembro no encontrado en esta familia");
        }
        return filas.getFirst();
    }

    private void protegerUltimoAdministrador(Long familiaInterna, MiembroActual actual, String permiso, boolean activo) {
        if (!actual.activo() || !"ADMINISTRADOR_FAMILIAR".equals(actual.rol())
                || (activo && "ADMINISTRADOR_FAMILIAR".equals(permiso))) return;
        Integer administradores = jdbc.queryForObject("SELECT COUNT(*) FROM miembros_familia WHERE familia_id=? AND activo AND rol='ADMINISTRADOR_FAMILIAR'",
                Integer.class, familiaInterna);
        if (administradores == null || administradores <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Asigna otro administrador antes de dar de baja o cambiar el rol del último administrador");
        }
    }

    private Long familiaInterna(UUID familiaId) {
        List<Long> familias = jdbc.query("SELECT id FROM familias WHERE id_publico=?", (rs, fila) -> rs.getLong(1), familiaId);
        if (familias.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Familia no encontrada");
        return familias.getFirst();
    }

    private void validarClave(String clave) {
        if (clave == null || clave.isBlank() || clave.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key inválida");
        }
    }

    private String validarPermiso(String permiso) {
        if (!"ADULTO".equals(permiso) && !"ADMINISTRADOR_FAMILIAR".equals(permiso)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permiso familiar inválido");
        }
        return permiso;
    }

    private UUID autorizar(Jwt jwt) {
        if (!ROL_ADMIN.equals(jwt.getClaimAsString("rol_plataforma"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso reservado a administración de plataforma");
        }
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identidad administrativa inválida");
        }
    }

    private String validarZona(String valor) {
        String zona = valor.trim();
        try {
            ZoneId.of(zona);
            return zona;
        } catch (ZoneRulesException error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Zona horaria inválida");
        }
    }

    private record MiembroActual(Long perfilInternoId, UUID usuarioId, String rol, boolean activo) { }
}
