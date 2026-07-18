package com.obusystem.agendafamiliar.agenda.catalogo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RespuestaCatalogo(List<MedicamentoResumen> medicamentos,
        List<TratamientoResumen> tratamientos, List<EventoResumen> eventos) {
    public record MedicamentoResumen(UUID id, String nombre, String presentacion, String concentracion,
            BigDecimal cantidad, String unidad, LocalDate fechaVencimiento, String estado) { }
    public record TratamientoResumen(UUID id, UUID perfilId, String persona, UUID medicamentoId,
            String medicamento, String indicacion, String dosisIndicada, String frecuencia,
            LocalDate fechaInicio, LocalDate fechaFin, String estado) { }
    public record EventoResumen(UUID id, UUID perfilId, String persona, String titulo, String tipo,
            String lugar, Instant inicioEn, Instant finEn, String estado) { }
}
