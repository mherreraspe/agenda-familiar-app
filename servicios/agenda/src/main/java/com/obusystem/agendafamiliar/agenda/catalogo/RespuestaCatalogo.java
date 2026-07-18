package com.obusystem.agendafamiliar.agenda.catalogo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record RespuestaCatalogo(List<MedicamentoResumen> medicamentos,
        List<TratamientoResumen> tratamientos, List<EventoResumen> eventos, List<LugarResumen> lugares) {
    public record MedicamentoResumen(UUID id, UUID loteId, String nombre, String presentacion, String concentracion,
            BigDecimal cantidad, String unidad, LocalDate fechaVencimiento, String estado) { }
    public record TratamientoResumen(UUID id, UUID perfilId, String persona, UUID medicamentoId,
            String medicamento, UUID responsablePerfilId, String responsable,
            UUID responsableAlternativoPerfilId, String responsableAlternativo,
            String indicacion, String dosisIndicada, String frecuencia, List<LocalTime> horarios, Integer intervaloHoras,
            LocalDate fechaInicio, LocalDate fechaFin, String estado) { }
    public record EventoResumen(UUID id, UUID perfilId, String persona, String titulo, String tipo,
            String lugar, String direccion, String notas, Instant inicioEn, Instant finEn, String estado) { }
    public record LugarResumen(UUID id, String nombre, String direccion, Instant ultimaUtilizacion,
            int frecuenciaUso) { }
}
