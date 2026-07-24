package com.obusystem.agendafamiliar.agenda.catalogo;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import com.obusystem.agendafamiliar.agenda.recurrencia.SolicitudRecurrencia;

public final class SolicitudesCatalogo {
    private SolicitudesCatalogo() { }

    public record Medicamento(@NotBlank @Size(max = 180) String nombre,
            @Size(max = 120) String presentacion, @Size(max = 120) String concentracion,
            @NotNull @DecimalMin("0") BigDecimal cantidad, @NotBlank @Size(max = 40) String unidad,
            @FutureOrPresent LocalDate fechaVencimiento,
            @Pattern(regexp = "SIN_ABRIR|ABIERTO") String estadoEnvase,
            LocalDate abiertoEn, @Min(1) @Max(3650) Integer duracionAbiertoDias,
            Boolean avisarVencimiento, @Min(0) @Max(365) Integer anticipacionVencimientoDias,
            Boolean avisarApertura, @Min(0) @Max(365) Integer anticipacionAperturaDias) {
        public Medicamento(String nombre, String presentacion, String concentracion, BigDecimal cantidad,
                String unidad, LocalDate fechaVencimiento) {
            this(nombre, presentacion, concentracion, cantidad, unidad, fechaVencimiento,
                    "SIN_ABRIR", null, null, true, 7, true, 3);
        }
    }

    public record Tratamiento(@NotNull UUID perfilId, UUID medicamentoId,
            @NotBlank @Size(max = 180) String nombre,
            @Size(max = 1000) String indicacion,
            @Size(max = 300) String cantidadReceta,
            @Size(max = 300) String frecuencia,
            LocalTime horario, @Size(max = 8) List<LocalTime> horarios,
            @Min(1) @Max(168) Integer intervaloHoras,
            LocalDate fechaInicio, LocalDate fechaFin, UUID responsablePerfilId,
            UUID responsableAlternativoPerfilId) { }

    public record TratamientoMultiple(@NotNull @Size(min = 1, max = 12) List<@NotNull UUID> perfilIds,
            UUID medicamentoId, @NotBlank @Size(max = 180) String nombre,
            @Size(max = 180) String nombreMedicamento, @Size(max = 300) String dosis,
            @Size(max = 300) String aplicacion, @Size(max = 1000) String indicacion,
            @Size(max = 300) String frecuencia, @NotNull @Size(min = 1, max = 8) List<@NotNull LocalTime> horarios,
            @Min(1) @Max(168) Integer intervaloHoras, LocalDate fechaInicio, LocalDate fechaFin,
            UUID responsablePerfilId, UUID responsableAlternativoPerfilId) { }

    public record ActualizacionTratamiento(UUID medicamentoId,
            @NotBlank @Size(max = 180) String nombre,
            @Size(max = 180) String nombreMedicamento, @Size(max = 300) String dosis,
            @Size(max = 300) String aplicacion, @Size(max = 1000) String indicacion,
            @Size(max = 300) String frecuencia,
            @NotNull @Size(min = 1, max = 8) List<@NotNull LocalTime> horarios,
            @Min(1) @Max(168) Integer intervaloHoras,
            @NotNull LocalDate fechaInicio, LocalDate fechaFin,
            UUID responsablePerfilId, UUID responsableAlternativoPerfilId) { }

    public record ActualizacionEnvase(
            @NotNull @Pattern(regexp = "SIN_ABRIR|ABIERTO") String estadoEnvase,
            LocalDate abiertoEn, @Min(1) @Max(3650) Integer duracionAbiertoDias,
            Boolean avisarVencimiento, @Min(0) @Max(365) Integer anticipacionVencimientoDias,
            Boolean avisarApertura, @Min(0) @Max(365) Integer anticipacionAperturaDias,
            @Pattern(regexp = "DISPONIBLE|AGOTADO|DESCARTADO") String estadoInventario,
            @NotNull @Min(0) Long version) { }

    public record Evento(UUID perfilId, @NotBlank @Size(max = 180) String titulo,
            @Size(max = 40) String tipo, @Size(max = 300) String lugar,
            @Size(max = 500) String direccion, @Size(max = 1000) String notas,
            @NotNull @FutureOrPresent Instant inicioEn, Instant finEn,
            @Valid SolicitudRecurrencia recurrencia, Boolean avisar24h, Boolean avisar1h) {
        public Evento(UUID perfilId, String titulo, String tipo, String lugar, String direccion,
                String notas, Instant inicioEn, Instant finEn) {
            this(perfilId, titulo, tipo, lugar, direccion, notas, inicioEn, finEn, null, true, true);
        }
        public Evento(UUID perfilId, String titulo, String tipo, String lugar, String direccion,
                String notas, Instant inicioEn, Instant finEn, SolicitudRecurrencia recurrencia) {
            this(perfilId, titulo, tipo, lugar, direccion, notas, inicioEn, finEn, recurrencia, true, true);
        }
    }
}
