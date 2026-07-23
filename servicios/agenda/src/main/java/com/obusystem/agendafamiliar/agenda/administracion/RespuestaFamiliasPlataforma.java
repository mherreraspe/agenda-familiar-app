package com.obusystem.agendafamiliar.agenda.administracion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RespuestaFamiliasPlataforma(List<FamiliaAdministrada> familias) {
    public record FamiliaAdministrada(UUID id, String nombre, String zonaHoraria, Instant creadaEn) { }
}
