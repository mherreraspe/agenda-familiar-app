package com.obusystem.agendafamiliar.agenda.hoy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RespuestaHoy(UUID familiaId, String familia, String zonaHoraria,
        List<PerfilResumen> perfiles, List<TareaResumen> tareas) {
    public record PerfilResumen(UUID id, String nombre, String tipo, String color, String relacion) { }
    public record TareaResumen(UUID id, String titulo, String descripcion, Instant fechaLimite,
            String estado, UUID perfilId, String responsable, boolean recurrente, UUID tareaOrigenId) { }
}
