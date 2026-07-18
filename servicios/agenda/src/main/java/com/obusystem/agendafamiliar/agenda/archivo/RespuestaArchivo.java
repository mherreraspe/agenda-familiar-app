package com.obusystem.agendafamiliar.agenda.archivo;

import java.time.Instant;
import java.util.UUID;

public record RespuestaArchivo(UUID id, UUID tratamientoId, int ancho, int alto,
        long bytesAlmacenados, Instant creadoEn) { }
