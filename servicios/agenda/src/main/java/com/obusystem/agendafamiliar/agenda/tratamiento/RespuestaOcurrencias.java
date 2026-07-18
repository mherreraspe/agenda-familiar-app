package com.obusystem.agendafamiliar.agenda.tratamiento;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RespuestaOcurrencias(List<OcurrenciaResumen> ocurrencias, List<ElementoRevision> revisar) {
    public record OcurrenciaResumen(UUID id, UUID tratamientoId, UUID perfilId, String persona,
            String tratamiento, Instant programadaEn, String estado, Instant pospuestaA,
            UUID resueltaPor, Instant resueltaEn) { }

    public record ElementoRevision(UUID id, String origen, UUID entidadId, String motivo,
            String titulo, Instant fecha, String estado) { }
}
