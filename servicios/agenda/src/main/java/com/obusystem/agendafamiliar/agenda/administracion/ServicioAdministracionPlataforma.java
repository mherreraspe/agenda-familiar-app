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

@Service
public class ServicioAdministracionPlataforma {
    private static final String ROL_ADMIN = "ADMINISTRADOR_PLATAFORMA";
    private final JdbcTemplate jdbc;

    public ServicioAdministracionPlataforma(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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

    private RespuestaFamiliasPlataforma.FamiliaAdministrada buscar(UUID id) {
        return jdbc.queryForObject("SELECT id_publico, nombre, zona_horaria, creado_en FROM familias WHERE id_publico=?",
                (rs, fila) -> new RespuestaFamiliasPlataforma.FamiliaAdministrada(
                        rs.getObject("id_publico", UUID.class), rs.getString("nombre"),
                        rs.getString("zona_horaria"), rs.getTimestamp("creado_en").toInstant()), id);
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
}
