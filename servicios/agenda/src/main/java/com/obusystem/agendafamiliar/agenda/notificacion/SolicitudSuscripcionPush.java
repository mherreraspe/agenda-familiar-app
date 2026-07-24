package com.obusystem.agendafamiliar.agenda.notificacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SolicitudSuscripcionPush(
        @NotBlank @Size(max = 2048) String endpoint,
        @NotBlank @Size(max = 512) String claveP256dh,
        @NotBlank @Size(max = 256) String claveAuth,
        @NotBlank @Size(max = 100) String dispositivo) { }
