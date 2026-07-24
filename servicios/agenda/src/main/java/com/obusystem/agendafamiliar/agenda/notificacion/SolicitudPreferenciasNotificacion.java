package com.obusystem.agendafamiliar.agenda.notificacion;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record SolicitudPreferenciasNotificacion(
        boolean tareas,
        boolean eventos,
        boolean salud,
        boolean botiquin,
        @NotNull LocalTime silencioDesde,
        @NotNull LocalTime silencioHasta) { }
