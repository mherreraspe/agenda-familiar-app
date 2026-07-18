package com.obusystem.agendafamiliar.agenda.busqueda;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.obusystem.agendafamiliar.agenda.family.ContextoFamilia;

@Service
public class ServicioIndexacion {
    static final String VERSION = "reglas-es-v1";
    private static final List<Regla> REGLAS = List.of(
            new Regla("pediatra", List.of("pediatra", "pediatrico", "pediatrica")),
            new Regla("vacuna", List.of("vacuna", "vacunacion")),
            new Regla("control", List.of("control")),
            new Regla("escuela", List.of("escuela", "colegio", "escolar")),
            new Regla("cumpleanos", List.of("cumpleanos")),
            new Regla("deporte", List.of("deporte", "futbol", "natacion")),
            new Regla("cita", List.of("cita", "consulta")));

    private final JdbcTemplate jdbc;
    private final ContextoFamilia contexto;

    public ServicioIndexacion(JdbcTemplate jdbc, ContextoFamilia contexto) {
        this.jdbc = jdbc;
        this.contexto = contexto;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void indexar(EventoCreado evento) {
        contexto.activar(evento.familiaId());
        List<String> textos = jdbc.query(
                "SELECT titulo || ' ' || COALESCE(tipo, '') FROM eventos WHERE familia_id=? AND id_publico=?",
                (rs, fila) -> rs.getString(1), evento.familiaId(), evento.eventoId());
        if (textos.isEmpty()) return;
        for (String palabra : extraer(textos.getFirst())) {
            jdbc.update("INSERT INTO palabras_clave (familia_id, palabra, origen, entidad, entidad_publica_id, version_extractor) VALUES (?, ?, 'REGLA', 'EVENTO', ?, ?) ON CONFLICT DO NOTHING",
                    evento.familiaId(), palabra, evento.eventoId(), VERSION);
        }
    }

    static Set<String> extraer(String texto) {
        String normalizado = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
        Set<String> resultado = new LinkedHashSet<>();
        for (Regla regla : REGLAS) {
            if (regla.terminos().stream().anyMatch(termino -> contienePalabra(normalizado, termino))) {
                resultado.add(regla.canonica());
                if (resultado.size() == 2) break;
            }
        }
        return resultado;
    }

    private static boolean contienePalabra(String texto, String termino) {
        return (" " + texto.replaceAll("[^a-z0-9]+", " ").trim() + " ").contains(" " + termino + " ");
    }

    private record Regla(String canonica, List<String> terminos) { }
}
