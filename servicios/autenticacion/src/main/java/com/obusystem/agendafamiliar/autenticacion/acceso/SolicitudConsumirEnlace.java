package com.obusystem.agendafamiliar.autenticacion.acceso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudConsumirEnlace(
        @NotBlank @Size(max = 200) String token,
        @NotBlank @Size(min = 12, max = 128) String clave) { }
