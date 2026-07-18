package com.obusystem.agendafamiliar.agenda.recurrencia;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.obusystem.agendafamiliar.agenda.util.UuidV7;

@Service
public class ServicioRecurrencias {
    private static final int MAXIMO_OCURRENCIAS = 100;
    private final JdbcTemplate jdbc;

    public ServicioRecurrencias(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Serie crear(Long familiaId, String entidad, Instant inicio, String zonaHoraria,
            SolicitudRecurrencia solicitud) {
        if (solicitud == null) return new Serie(null, List.of(inicio));
        if (!solicitud.hasta().isAfter(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La recurrencia debe terminar después del inicio");
        }
        List<Instant> fechas = fechas(inicio, solicitud, ZoneId.of(zonaHoraria));
        UUID publica = UuidV7.nuevo();
        Long id = jdbc.queryForObject("INSERT INTO recurrencias_agenda (id_publico, familia_id, entidad, frecuencia, intervalo, hasta) VALUES (?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class, publica, familiaId, entidad, solicitud.frecuencia().name(), solicitud.intervalo(),
                Timestamp.from(solicitud.hasta()));
        return new Serie(id, fechas);
    }

    private List<Instant> fechas(Instant inicio, SolicitudRecurrencia solicitud, ZoneId zona) {
        List<Instant> fechas = new ArrayList<>();
        ZonedDateTime actual = inicio.atZone(zona);
        ZonedDateTime limite = solicitud.hasta().atZone(zona);
        while (!actual.isAfter(limite) && fechas.size() < MAXIMO_OCURRENCIAS) {
            fechas.add(actual.toInstant());
            actual = switch (solicitud.frecuencia()) {
                case DIARIA -> actual.plusDays(solicitud.intervalo());
                case SEMANAL -> actual.plusWeeks(solicitud.intervalo());
                case MENSUAL -> actual.plusMonths(solicitud.intervalo());
            };
        }
        if (!actual.isAfter(limite)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La recurrencia supera el máximo de 100 ocurrencias");
        }
        return List.copyOf(fechas);
    }

    public record Serie(Long id, List<Instant> fechas) { }
}
