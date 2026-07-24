package com.obusystem.agendafamiliar.agenda.administracion;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudMiembroPlataforma(
        @NotNull UUID usuarioId,
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank String permiso) { }
