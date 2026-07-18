package com.obusystem.agendafamiliar.agenda.recurrencia;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SolicitudRecurrencia(@NotNull Frecuencia frecuencia,
        @Min(1) @Max(30) int intervalo, @NotNull @Future Instant hasta) {
    public enum Frecuencia { DIARIA, SEMANAL, MENSUAL }
}
