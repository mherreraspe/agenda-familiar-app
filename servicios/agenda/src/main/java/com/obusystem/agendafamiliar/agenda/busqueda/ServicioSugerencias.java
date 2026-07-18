package com.obusystem.agendafamiliar.agenda.busqueda;

import java.util.Locale;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obusystem.agendafamiliar.agenda.family.AccesoFamilia;
import com.obusystem.agendafamiliar.agenda.family.Familia;

@Service
public class ServicioSugerencias {
    private final AccesoFamilia acceso;
    private final JdbcTemplate jdbc;

    public ServicioSugerencias(AccesoFamilia acceso, JdbcTemplate jdbc) {
        this.acceso = acceso;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public RespuestaSugerencias consultar(UUID familiaId, String consulta, Jwt jwt) {
        Familia familia = acceso.autorizar(familiaId, jwt);
        String termino = consulta == null ? "" : consulta.trim().toLowerCase(Locale.ROOT);
        if (termino.length() < 2) return new RespuestaSugerencias(java.util.List.of());
        if (termino.length() > 100) termino = termino.substring(0, 100);
        String contiene = "%" + termino.replace("%", "").replace("_", "") + "%";
        var sugerencias = jdbc.query("SELECT tipo, entidad_id, titulo, lugar, direccion FROM ("
                + "SELECT 'EVENTO' tipo, e.id_publico entidad_id, e.titulo, e.lugar, e.direccion, e.inicio_en orden "
                + "FROM eventos e WHERE e.familia_id=? AND (LOWER(e.titulo) LIKE ? OR LOWER(COALESCE(e.lugar,'')) LIKE ? "
                + "OR EXISTS (SELECT 1 FROM palabras_clave pc WHERE pc.familia_id=e.familia_id AND pc.entidad='EVENTO' AND pc.entidad_publica_id=e.id_publico AND pc.palabra LIKE ?)) "
                + "UNION ALL SELECT 'LUGAR', l.id_publico, l.nombre, l.nombre, l.direccion, l.ultima_utilizacion "
                + "FROM lugares_familia l WHERE l.familia_id=? AND (l.nombre_normalizado LIKE ? OR similarity(l.nombre_normalizado, ?) > 0.25)"
                + ") reales ORDER BY orden DESC LIMIT 10",
                (rs, fila) -> new RespuestaSugerencias.Sugerencia(rs.getString("tipo"),
                        rs.getObject("entidad_id", UUID.class), rs.getString("titulo"), rs.getString("lugar"),
                        rs.getString("direccion")), familia.getId(), contiene, contiene, termino + "%",
                familia.getId(), contiene, termino);
        return new RespuestaSugerencias(sugerencias);
    }
}
