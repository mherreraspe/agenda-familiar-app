package com.obusystem.agendafamiliar.agenda.family;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccesoFamilia {
    private final RepositorioFamilias familias;
    private final RepositorioMiembrosFamilia miembros;
    private final ContextoFamilia contexto;

    public AccesoFamilia(RepositorioFamilias familias, RepositorioMiembrosFamilia miembros, ContextoFamilia contexto) {
        this.familias = familias;
        this.miembros = miembros;
        this.contexto = contexto;
    }

    public Familia autorizar(UUID familiaId, Jwt jwt) {
        Familia familia = familias.findByIdPublico(familiaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Familia no encontrada"));
        contexto.activar(familia.getId());
        UUID usuarioId;
        try {
            usuarioId = UUID.fromString(jwt.getSubject());
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identidad inválida");
        }
        if (!miembros.existsByFamiliaIdAndUsuarioPublicoIdAndActivoTrue(familia.getId(), usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Familia no encontrada");
        }
        return familia;
    }
}
