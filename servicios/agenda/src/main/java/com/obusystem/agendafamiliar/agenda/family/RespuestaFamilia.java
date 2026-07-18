package com.obusystem.agendafamiliar.agenda.family;

import java.util.List;
import java.util.UUID;

public record RespuestaFamilia(boolean puedeAdministrar, List<PerfilAdministrado> perfiles) {
    public record PerfilAdministrado(UUID id, String nombre, String tipo, String color, String relacion,
            UUID usuarioId, String permiso, boolean activo) { }
}
