package com.obusystem.agendafamiliar.autenticacion.sesion;

import java.util.UUID;

public record RespuestaUsuario(UUID usuarioId, String correo) {
}
