package com.obusystem.agendafamiliar.autenticacion.usuario;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InicializadorFamiliaTest implements ApplicationRunner {
    public static final UUID PAPA_ID = UUID.fromString("0197f100-0000-7000-8000-000000000101");
    public static final UUID MAMA_ID = UUID.fromString("0197f100-0000-7000-8000-000000000102");

    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder claves;
    private final boolean habilitada;
    private final String claveInicial;

    public InicializadorFamiliaTest(
            RepositorioUsuarios usuarios,
            PasswordEncoder claves,
            @Value("${familia-test.habilitada}") boolean habilitada,
            @Value("${familia-test.clave}") String claveInicial) {
        this.usuarios = usuarios;
        this.claves = claves;
        this.habilitada = habilitada;
        this.claveInicial = claveInicial;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!habilitada) {
            return;
        }
        if (claveInicial == null || claveInicial.length() < 12) {
            throw new IllegalStateException("FAMILIA_TEST_PASSWORD debe contener al menos 12 caracteres");
        }
        crearSiFalta(PAPA_ID, "papa@familia.test");
        crearSiFalta(MAMA_ID, "mama@familia.test");
    }

    private void crearSiFalta(UUID id, String correo) {
        if (usuarios.findByCorreoIgnoreCase(correo).isEmpty()) {
            usuarios.save(new Usuario(id, correo, claves.encode(claveInicial)));
        }
    }
}
