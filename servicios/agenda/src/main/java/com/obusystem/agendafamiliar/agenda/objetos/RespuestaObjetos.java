package com.obusystem.agendafamiliar.agenda.objetos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RespuestaObjetos(List<Objeto> objetos, List<Ubicacion> ubicaciones) {
    public record Objeto(UUID id, String nombre, String categoria, String notas, List<String> ruta,
            Instant actualizadoEn, long version) { }
    public record Ubicacion(List<String> ruta, int cantidad) { }
}
