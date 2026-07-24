package com.obusystem.agendafamiliar.agenda.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class ServicioFamiliasUsuarioTest {
    @Test
    void filtraMembresiasConRlsYOrdenaLasFamiliasAccesibles() {
        UUID usuario = UUID.randomUUID();
        Familia zeta = familia(2L, UUID.randomUUID(), "Zeta");
        Familia alfa = familia(1L, UUID.randomUUID(), "Alfa");
        RepositorioFamilias familias = mock(RepositorioFamilias.class);
        RepositorioMiembrosFamilia miembros = mock(RepositorioMiembrosFamilia.class);
        ContextoFamilia contexto = mock(ContextoFamilia.class);
        MiembroFamilia miembro = mock(MiembroFamilia.class);
        when(miembro.getRol()).thenReturn("ADULTO");
        when(familias.findAll()).thenReturn(List.of(zeta, alfa));
        when(miembros.findByFamiliaIdAndUsuarioPublicoIdAndActivoTrue(2L, usuario)).thenReturn(Optional.empty());
        when(miembros.findByFamiliaIdAndUsuarioPublicoIdAndActivoTrue(1L, usuario)).thenReturn(Optional.of(miembro));

        var respuesta = new ServicioFamiliasUsuario(familias, miembros, contexto).consultar(jwt(usuario));

        assertThat(respuesta.familias()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(alfa.getIdPublico());
            assertThat(item.nombre()).isEqualTo("Alfa");
            assertThat(item.rol()).isEqualTo("ADULTO");
        });
        verify(contexto).activar(2L);
        verify(contexto).activar(1L);
    }

    private Familia familia(Long id, UUID idPublico, String nombre) {
        Familia familia = mock(Familia.class);
        when(familia.getId()).thenReturn(id);
        when(familia.getIdPublico()).thenReturn(idPublico);
        when(familia.getNombre()).thenReturn(nombre);
        when(familia.getZonaHoraria()).thenReturn("America/Lima");
        return familia;
    }

    private Jwt jwt(UUID usuario) {
        Instant ahora = Instant.now();
        return new Jwt("token", ahora, ahora.plusSeconds(300), Map.of("alg", "none"), Map.of("sub", usuario.toString()));
    }
}
