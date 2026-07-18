package com.obusystem.agendafamiliar.autenticacion.sesion;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ManejadorErroresAutenticacion {
    @ExceptionHandler(BadCredentialsException.class)
    ProblemDetail credencialesInvalidas() {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Correo o clave incorrectos");
        problema.setTitle("No se pudo iniciar sesión");
        problema.setType(URI.create("https://www.obusystem.com/problemas/credenciales-invalidas"));
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail solicitudInvalida() {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Revisa el correo y la clave");
        problema.setTitle("Solicitud inválida");
        return problema;
    }

    @ExceptionHandler(ExcepcionCsrf.class)
    ProblemDetail csrfInvalido(ExcepcionCsrf excepcion) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, excepcion.getMessage());
        problema.setTitle("Solicitud no autorizada");
        problema.setType(URI.create("https://www.obusystem.com/problemas/csrf-invalido"));
        return problema;
    }
}
