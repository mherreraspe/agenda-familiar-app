package com.obusystem.agendafamiliar.autenticacion.sesion;

public class ExcepcionCsrf extends RuntimeException {
    public ExcepcionCsrf() {
        super("La comprobación de seguridad de la sesión falló");
    }
}
