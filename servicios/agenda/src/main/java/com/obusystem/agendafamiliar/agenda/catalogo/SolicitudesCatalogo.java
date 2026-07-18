package com.obusystem.agendafamiliar.agenda.catalogo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class SolicitudesCatalogo {
    private SolicitudesCatalogo() { }

    public record Medicamento(@NotBlank @Size(max = 180) String nombre,
            @Size(max = 120) String presentacion, @Size(max = 120) String concentracion,
            @NotNull @DecimalMin("0") BigDecimal cantidad, @NotBlank @Size(max = 40) String unidad,
            @FutureOrPresent LocalDate fechaVencimiento) { }

    public record Tratamiento(@NotNull UUID perfilId, @NotNull UUID medicamentoId,
            @NotBlank @Size(max = 1000) String indicacion,
            @NotBlank @Size(max = 300) String dosisIndicada,
            @NotBlank @Size(max = 300) String frecuencia,
            @NotNull LocalDate fechaInicio, LocalDate fechaFin) { }

    public record Evento(@NotNull UUID perfilId, @NotBlank @Size(max = 180) String titulo,
            @NotBlank @Size(max = 40) String tipo, @Size(max = 300) String lugar,
            @NotNull @FutureOrPresent Instant inicioEn, Instant finEn) { }
}
