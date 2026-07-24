package com.obusystem.agendafamiliar.autenticacion.acceso;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class RespuestaEnlace {
    private RespuestaEnlace() { }

    public record Generado(UUID id, String tipo, UUID usuarioId, String enlace, Instant expiraEn) { }
    public record Publico(String tipo, String correo, String familia, Instant expiraEn) { }
    public record Administrado(UUID id, String tipo, UUID usuarioId, String correo, String estado,
            Instant expiraEn, Instant creadoEn) { }
    public record Lista(List<Administrado> enlaces) { }
    public record Cuenta(UUID usuarioId, String correo, String estado) { }
    public record Cuentas(List<Cuenta> cuentas) { }
}
