package com.obusystem.agendafamiliar.autenticacion.acceso;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudToken(@NotBlank @Size(max = 200) String token) { }
