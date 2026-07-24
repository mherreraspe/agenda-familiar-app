package com.obusystem.agendafamiliar.agenda.hoy;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import com.obusystem.agendafamiliar.agenda.recurrencia.SolicitudRecurrencia;

public record SolicitudTarea(
        @NotBlank @Size(max = 180) String titulo,
        @Size(max = 1000) String descripcion,
        @NotNull UUID perfilId,
        @NotNull @FutureOrPresent Instant fechaLimite,
        @Valid SolicitudRecurrencia recurrencia,
        Boolean avisar) {
    public SolicitudTarea(String titulo, String descripcion, UUID perfilId, Instant fechaLimite) {
        this(titulo, descripcion, perfilId, fechaLimite, null, true);
    }
    public SolicitudTarea(String titulo, String descripcion, UUID perfilId, Instant fechaLimite,
            SolicitudRecurrencia recurrencia) {
        this(titulo, descripcion, perfilId, fechaLimite, recurrencia, true);
    }
}
