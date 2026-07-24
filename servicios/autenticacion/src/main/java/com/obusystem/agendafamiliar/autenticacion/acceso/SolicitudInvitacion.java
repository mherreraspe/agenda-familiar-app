package com.obusystem.agendafamiliar.autenticacion.acceso;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudInvitacion(
        @NotNull UUID usuarioId,
        @NotNull UUID familiaId,
        @NotBlank @Size(max = 120) String familiaNombre,
        @NotBlank @Email @Size(max = 320) String correo) { }
