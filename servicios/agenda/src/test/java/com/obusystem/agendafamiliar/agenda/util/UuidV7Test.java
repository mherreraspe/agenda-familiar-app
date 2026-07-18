package com.obusystem.agendafamiliar.agenda.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UuidV7Test {
    @Test
    void generaUuidVersionSieteConVarianteEstandar() {
        var id = UuidV7.nuevo();
        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }
}
