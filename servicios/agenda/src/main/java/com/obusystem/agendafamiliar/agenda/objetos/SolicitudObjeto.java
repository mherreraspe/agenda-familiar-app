package com.obusystem.agendafamiliar.agenda.objetos;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record SolicitudObjeto(
        @NotBlank @Size(max = 180) String nombre,
        @NotBlank @Size(max = 80) String categoria,
        @Size(max = 500) String notas,
        @PositiveOrZero Long version,
        @NotEmpty @Size(max = 5) List<@NotBlank @Size(max = 120) String> ruta) { }
