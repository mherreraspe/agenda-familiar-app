package com.obusystem.agendafamiliar.autenticacion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AplicacionAutenticacionTest {
    @Test
    void declaraPuntoDeEntrada() {
        assertThat(AplicacionAutenticacion.class).isNotNull();
    }
}
