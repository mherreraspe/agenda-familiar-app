package com.obusystem.agendafamiliar.autenticacion.sesion;

import java.time.Instant;
import java.util.UUID;

public record RespuestaSesion(String accessToken, Instant expiraEn, UUID usuarioId, String correo) {
}
