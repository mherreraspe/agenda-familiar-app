package com.obusystem.agendafamiliar.agenda.family;

import java.util.Comparator;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ServicioFamiliasUsuario {
    private final RepositorioFamilias familias;
    private final RepositorioMiembrosFamilia miembros;
    private final ContextoFamilia contexto;

    public ServicioFamiliasUsuario(RepositorioFamilias familias, RepositorioMiembrosFamilia miembros,
            ContextoFamilia contexto) {
        this.familias = familias;
        this.miembros = miembros;
        this.contexto = contexto;
    }

    @Transactional(readOnly = true)
    public RespuestaFamiliasUsuario consultar(Jwt jwt) {
        UUID usuarioId = usuarioId(jwt);
        var accesibles = familias.findAll().stream().map(familia -> {
            contexto.activar(familia.getId());
            return miembros.findByFamiliaIdAndUsuarioPublicoIdAndActivoTrue(familia.getId(), usuarioId)
                    .map(miembro -> new RespuestaFamiliasUsuario.FamiliaUsuario(
                            familia.getIdPublico(), familia.getNombre(), familia.getZonaHoraria(), miembro.getRol()))
                    .orElse(null);
        }).filter(java.util.Objects::nonNull)
                .sorted(Comparator.comparing(RespuestaFamiliasUsuario.FamiliaUsuario::nombre,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new RespuestaFamiliasUsuario(accesibles);
    }

    private UUID usuarioId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Identidad inválida");
        }
    }
}
