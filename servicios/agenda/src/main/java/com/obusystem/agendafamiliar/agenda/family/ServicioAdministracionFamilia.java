package com.obusystem.agendafamiliar.agenda.family;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.obusystem.agendafamiliar.agenda.util.UuidV7;

@Service
public class ServicioAdministracionFamilia {
    private final AccesoFamilia acceso;
    private final JdbcTemplate jdbc;

    public ServicioAdministracionFamilia(AccesoFamilia acceso, JdbcTemplate jdbc) {
        this.acceso = acceso;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public RespuestaFamilia consultar(UUID familiaId, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        UUID actor = UUID.fromString(jwt.getSubject());
        boolean administra = jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM miembros_familia WHERE familia_id=? AND usuario_publico_id=? AND activo AND rol='ADMINISTRADOR_FAMILIAR')",
                Boolean.class, familia.getId(), actor);
        return respuesta(familia.getId(), administra);
    }

    @Transactional
    public RespuestaFamilia.PerfilAdministrado crear(UUID familiaId, SolicitudPerfil solicitud, Jwt jwt) {
        Familia familia = acceso.autorizarAdministrador(familiaId, jwt);
        validarVinculo(solicitud);
        UUID id = UuidV7.nuevo();
        jdbc.update("INSERT INTO perfiles (id_publico, familia_id, nombre_visible, tipo, color, relacion, usuario_publico_id, activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, familia.getId(), solicitud.nombre().trim(), solicitud.tipo(), limpiar(solicitud.color()),
                limpiar(solicitud.relacion()), solicitud.usuarioId(), solicitud.activo());
        sincronizarMiembro(familia.getId(), id, solicitud);
        auditar(familia.getId(), jwt, "CREAR", id, "Perfil familiar registrado");
        return buscar(familia.getId(), id);
    }

    @Transactional
    public RespuestaFamilia.PerfilAdministrado actualizar(UUID familiaId, UUID perfilId,
            SolicitudPerfil solicitud, Jwt jwt) {
        Familia familia = acceso.autorizarAdministrador(familiaId, jwt);
        validarVinculo(solicitud);
        PerfilActual actual = actual(familia.getId(), perfilId);
        protegerUltimoAdministrador(familia.getId(), actual, solicitud);
        int cambio = jdbc.update("UPDATE perfiles SET nombre_visible=?, tipo=?, color=?, relacion=?, usuario_publico_id=?, activo=?, actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND id_publico=?",
                solicitud.nombre().trim(), solicitud.tipo(), limpiar(solicitud.color()), limpiar(solicitud.relacion()),
                solicitud.usuarioId(), solicitud.activo(), familia.getId(), perfilId);
        if (cambio == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado");
        if (actual.usuarioId() != null && !actual.usuarioId().equals(solicitud.usuarioId())) {
            jdbc.update("UPDATE miembros_familia SET activo=FALSE, perfil_id=NULL, actualizado_en=NOW(), version=version+1 WHERE familia_id=? AND usuario_publico_id=?",
                    familia.getId(), actual.usuarioId());
        }
        sincronizarMiembro(familia.getId(), perfilId, solicitud);
        auditar(familia.getId(), jwt, "ACTUALIZAR", perfilId, "Perfil y permisos familiares actualizados");
        return buscar(familia.getId(), perfilId);
    }

    private RespuestaFamilia respuesta(Long familiaId, boolean administra) {
        return new RespuestaFamilia(administra, jdbc.query("SELECT p.id_publico, p.nombre_visible, p.tipo, p.color, p.relacion, p.usuario_publico_id, m.rol, p.activo FROM perfiles p LEFT JOIN miembros_familia m ON m.familia_id=p.familia_id AND m.usuario_publico_id=p.usuario_publico_id WHERE p.familia_id=? ORDER BY p.activo DESC, p.nombre_visible",
                (rs, fila) -> new RespuestaFamilia.PerfilAdministrado(rs.getObject("id_publico", UUID.class),
                        rs.getString("nombre_visible"), rs.getString("tipo"), rs.getString("color"),
                        rs.getString("relacion"), rs.getObject("usuario_publico_id", UUID.class),
                        rs.getString("rol"), rs.getBoolean("activo")), familiaId));
    }

    private RespuestaFamilia.PerfilAdministrado buscar(Long familiaId, UUID perfilId) {
        return respuesta(familiaId, true).perfiles().stream().filter(p -> p.id().equals(perfilId)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado"));
    }

    private PerfilActual actual(Long familiaId, UUID perfilId) {
        List<PerfilActual> filas = jdbc.query("SELECT p.usuario_publico_id, m.rol FROM perfiles p LEFT JOIN miembros_familia m ON m.familia_id=p.familia_id AND m.usuario_publico_id=p.usuario_publico_id WHERE p.familia_id=? AND p.id_publico=?",
                (rs, fila) -> new PerfilActual(rs.getObject("usuario_publico_id", UUID.class), rs.getString("rol")),
                familiaId, perfilId);
        if (filas.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil no encontrado");
        return filas.getFirst();
    }

    private void validarVinculo(SolicitudPerfil solicitud) {
        if ("DEPENDIENTE".equals(solicitud.tipo()) && (solicitud.usuarioId() != null || solicitud.permiso() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un dependiente no puede tener cuenta ni permisos de adulto");
        }
        if (solicitud.permiso() != null && solicitud.usuarioId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El permiso requiere una cuenta vinculada");
        }
    }

    private void sincronizarMiembro(Long familiaId, UUID perfilId, SolicitudPerfil solicitud) {
        if (solicitud.usuarioId() == null) return;
        String rol = solicitud.permiso() == null ? "ADULTO" : solicitud.permiso();
        Long perfilInterno = jdbc.queryForObject("SELECT id FROM perfiles WHERE familia_id=? AND id_publico=?", Long.class,
                familiaId, perfilId);
        jdbc.update("INSERT INTO miembros_familia (familia_id, usuario_publico_id, rol, activo, perfil_id) VALUES (?, ?, ?, ?, ?) ON CONFLICT (familia_id, usuario_publico_id) DO UPDATE SET rol=EXCLUDED.rol, activo=EXCLUDED.activo, perfil_id=EXCLUDED.perfil_id, actualizado_en=NOW(), version=miembros_familia.version+1",
                familiaId, solicitud.usuarioId(), rol, solicitud.activo(), perfilInterno);
    }

    private void protegerUltimoAdministrador(Long familiaId, PerfilActual actual, SolicitudPerfil solicitud) {
        if (!"ADMINISTRADOR_FAMILIAR".equals(actual.rol())) return;
        boolean dejaDeSerlo = !solicitud.activo() || solicitud.usuarioId() == null
                || !actual.usuarioId().equals(solicitud.usuarioId())
                || !"ADMINISTRADOR_FAMILIAR".equals(solicitud.permiso());
        if (!dejaDeSerlo) return;
        Integer administradores = jdbc.queryForObject("SELECT COUNT(*) FROM miembros_familia WHERE familia_id=? AND activo AND rol='ADMINISTRADOR_FAMILIAR'",
                Integer.class, familiaId);
        if (administradores <= 1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La familia debe conservar al menos un administrador activo");
    }

    private String limpiar(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }

    private void auditar(Long familiaId, Jwt jwt, String operacion, UUID id, String resumen) {
        jdbc.update("INSERT INTO auditoria (familia_id, actor_publico_id, operacion, entidad, entidad_publica_id, resumen_seguro) VALUES (?, ?, ?, 'PERFIL', ?, ?)",
                familiaId, UUID.fromString(jwt.getSubject()), operacion, id, resumen);
    }

    private record PerfilActual(UUID usuarioId, String rol) { }
}
