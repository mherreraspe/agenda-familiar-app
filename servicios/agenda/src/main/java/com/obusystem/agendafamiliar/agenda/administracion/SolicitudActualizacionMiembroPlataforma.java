package com.obusystem.agendafamiliar.agenda.administracion;

import jakarta.validation.constraints.NotBlank;

public record SolicitudActualizacionMiembroPlataforma(
        @NotBlank String permiso,
        boolean activo) { }
