package com.obusystem.agendafamiliar.agenda.auditoria;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RespuestaAuditoria(List<EntradaAuditoria> entradas) {
    public record EntradaAuditoria(String operacion, String entidad, UUID entidadId, String titulo,
            UUID actorId, String actor, String resumen, Instant fecha) { }
}
