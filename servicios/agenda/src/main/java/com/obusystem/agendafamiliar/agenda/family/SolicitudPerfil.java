package com.obusystem.agendafamiliar.agenda.family;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SolicitudPerfil(@NotBlank @Size(max = 120) String nombre,
        @NotNull @Pattern(regexp = "ADULTO|DEPENDIENTE") String tipo,
        @Size(max = 20) String color, @Size(max = 80) String relacion,
        UUID usuarioId, @Pattern(regexp = "ADMINISTRADOR_FAMILIAR|ADULTO") String permiso,
        boolean activo) { }
