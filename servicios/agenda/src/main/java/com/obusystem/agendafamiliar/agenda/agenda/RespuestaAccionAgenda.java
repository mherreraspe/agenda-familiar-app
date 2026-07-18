package com.obusystem.agendafamiliar.agenda.agenda;

import java.time.Instant;
import java.util.UUID;

public record RespuestaAccionAgenda(UUID id, UUID origenId, String entidad, String estado, Instant fecha) { }
