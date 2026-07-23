package com.obusystem.agendafamiliar.agenda.administracion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudFamiliaPlataforma(
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Size(max = 60) String zonaHoraria) {
}
