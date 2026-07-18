package com.obusystem.agendafamiliar.agenda.auditoria;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.obusystem.agendafamiliar.agenda.family.AccesoFamilia;
import com.obusystem.agendafamiliar.agenda.family.Familia;

@Service
public class ServicioAuditoria {
    private final AccesoFamilia acceso;
    private final JdbcTemplate jdbc;

    public ServicioAuditoria(AccesoFamilia acceso, JdbcTemplate jdbc) {
        this.acceso = acceso;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public RespuestaAuditoria consultar(UUID familiaId, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        UUID usuario = UUID.fromString(jwt.getSubject());
        Integer adultos = jdbc.queryForObject("SELECT COUNT(*) FROM miembros_familia m LEFT JOIN perfiles p ON p.id=m.perfil_id WHERE m.familia_id=? AND m.usuario_publico_id=? AND m.activo AND (p.tipo='ADULTO' OR m.rol IN ('ADULTO','ADMINISTRADOR_FAMILIAR'))",
                Integer.class, familia.getId(), usuario);
        if (adultos == null || adultos == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El historial está disponible para adultos autorizados");
        }
        var entradas = jdbc.query("SELECT a.operacion, a.entidad, a.entidad_publica_id, a.actor_publico_id, "
                + "COALESCE(actor.nombre_visible, 'Adulto autorizado') actor, a.resumen_seguro, a.creado_en, "
                + "COALESCE(e.titulo, t.nombre_libre, med.nombre, ta.titulo, ot.nombre_libre, 'Registro familiar') titulo "
                + "FROM auditoria a "
                + "LEFT JOIN miembros_familia mf ON mf.familia_id=a.familia_id AND mf.usuario_publico_id=a.actor_publico_id "
                + "LEFT JOIN perfiles actor ON actor.id=mf.perfil_id "
                + "LEFT JOIN eventos e ON a.entidad='EVENTO' AND e.id_publico=a.entidad_publica_id "
                + "LEFT JOIN tratamientos t ON a.entidad='TRATAMIENTO' AND t.id_publico=a.entidad_publica_id "
                + "LEFT JOIN medicamentos med ON a.entidad='MEDICAMENTO' AND med.id_publico=a.entidad_publica_id "
                + "LEFT JOIN tareas ta ON a.entidad='TAREA' AND ta.id_publico=a.entidad_publica_id "
                + "LEFT JOIN ocurrencias_tratamiento o ON a.entidad='OCURRENCIA_TRATAMIENTO' AND o.id_publico=a.entidad_publica_id "
                + "LEFT JOIN tratamientos ot ON ot.id=o.tratamiento_id "
                + "WHERE a.familia_id=? ORDER BY a.creado_en DESC LIMIT 100",
                (rs, fila) -> new RespuestaAuditoria.EntradaAuditoria(rs.getString("operacion"),
                        rs.getString("entidad"), rs.getObject("entidad_publica_id", UUID.class),
                        rs.getString("titulo"), rs.getObject("actor_publico_id", UUID.class), rs.getString("actor"),
                        rs.getString("resumen_seguro"), rs.getTimestamp("creado_en").toInstant()), familia.getId());
        return new RespuestaAuditoria(entradas);
    }
}
