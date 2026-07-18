package com.obusystem.agendafamiliar.agenda.busqueda;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ServicioIndexacionTest {
    @Test
    void extraeComoMaximoDosCanonicasDeListaConservadora() {
        assertThat(ServicioIndexacion.extraer("Control pediátrico y vacuna"))
                .containsExactly("pediatra", "vacuna");
    }

    @Test
    void noConvierteNombresDireccionesDiagnosticosNiDosisEnPalabrasClave() {
        assertThat(ServicioIndexacion.extraer("Ana, calle Norte 25, diagnóstico reservado, 20 mg"))
                .isEmpty();
    }
}
