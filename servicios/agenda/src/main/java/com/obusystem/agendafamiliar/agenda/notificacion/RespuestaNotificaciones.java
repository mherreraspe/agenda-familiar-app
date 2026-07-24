package com.obusystem.agendafamiliar.agenda.notificacion;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record RespuestaNotificaciones(
        List<Aviso> avisos,
        long sinLeer,
        Preferencias preferencias,
        List<Dispositivo> dispositivos,
        boolean pushDisponible,
        String clavePublica) {

    public record Aviso(UUID id, String tipo, String titulo, String detalle, String destino,
            Instant creadaEn, Instant leidaEn) { }

    public record Preferencias(boolean tareas, boolean eventos, boolean salud, boolean botiquin,
            LocalTime silencioDesde, LocalTime silencioHasta) { }

    public record Dispositivo(UUID id, String nombre, boolean activo, Instant creadoEn,
            Instant ultimoExitoEn) { }
}
