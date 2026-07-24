package com.obusystem.agendafamiliar.agenda.administracion;

import java.util.List;
import java.util.UUID;

public record RespuestaMiembrosPlataforma(List<MiembroAdministrado> miembros) {
    public record MiembroAdministrado(UUID perfilId, UUID usuarioId, String nombre, String permiso, boolean activo) { }
}
