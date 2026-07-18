package com.obusystem.agendafamiliar.agenda.busqueda;

import java.util.List;
import java.util.UUID;

public record RespuestaSugerencias(List<Sugerencia> sugerencias) {
    public record Sugerencia(String tipo, UUID entidadId, String titulo, String lugar, String direccion) { }
}
