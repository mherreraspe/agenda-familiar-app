package com.obusystem.agendafamiliar.agenda.family;

import java.util.List;
import java.util.UUID;

public record RespuestaFamiliasUsuario(List<FamiliaUsuario> familias) {
    public record FamiliaUsuario(UUID id, String nombre, String zonaHoraria, String rol) { }
}
