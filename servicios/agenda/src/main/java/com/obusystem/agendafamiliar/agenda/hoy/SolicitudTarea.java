package com.obusystem.agendafamiliar.agenda.hoy;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudTarea(
        @NotBlank @Size(max = 180) String titulo,
        @Size(max = 1000) String descripcion,
        @NotNull UUID perfilId,
        @NotNull @FutureOrPresent Instant fechaLimite) {
}
