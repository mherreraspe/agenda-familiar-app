package com.obusystem.agendafamiliar.autenticacion.sesion;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
public class ControladorSesiones {
    static final String COOKIE_REFRESH = "refresh_token";
    static final String COOKIE_CSRF = "XSRF-TOKEN";
    static final String HEADER_CSRF = "X-XSRF-TOKEN";

    private final ServicioSesiones sesiones;
    private final boolean cookieSegura;

    public ControladorSesiones(ServicioSesiones sesiones,
            @Value("${seguridad.cookies.secure}") boolean cookieSegura) {
        this.sesiones = sesiones;
        this.cookieSegura = cookieSegura;
    }

    @PostMapping("/iniciar-sesion")
    ResponseEntity<RespuestaSesion> iniciar(@Valid @RequestBody SolicitudInicioSesion solicitud) {
        return respuestaConCookies(sesiones.iniciar(solicitud));
    }

    @PostMapping("/renovar")
    ResponseEntity<RespuestaSesion> renovar(
            @CookieValue(name = COOKIE_REFRESH, required = false) String refresh,
            @CookieValue(name = COOKIE_CSRF, required = false) String csrfCookie,
            @RequestHeader(name = HEADER_CSRF, required = false) String csrfHeader) {
        return respuestaConCookies(sesiones.renovar(refresh, csrfCookie, csrfHeader));
    }

    @PostMapping("/cerrar-sesion")
    ResponseEntity<Void> cerrar(
            @CookieValue(name = COOKIE_REFRESH, required = false) String refresh,
            @CookieValue(name = COOKIE_CSRF, required = false) String csrfCookie,
            @RequestHeader(name = HEADER_CSRF, required = false) String csrfHeader) {
        sesiones.cerrar(refresh, csrfCookie, csrfHeader);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieRefresh("", Duration.ZERO).toString())
                .header(HttpHeaders.SET_COOKIE, cookieCsrf("", Duration.ZERO).toString())
                .build();
    }

    @GetMapping("/usuario-actual")
    RespuestaUsuario usuarioActual(@AuthenticationPrincipal Jwt jwt) {
        return sesiones.usuarioActual(UUID.fromString(jwt.getSubject()));
    }

    private ResponseEntity<RespuestaSesion> respuestaConCookies(ServicioSesiones.PaqueteSesion paquete) {
        Duration maxAge = Duration.between(Instant.now(), paquete.expiraRefresh());
        return ResponseEntity.status(HttpStatus.OK)
                .header(HttpHeaders.SET_COOKIE, cookieRefresh(paquete.refreshToken(), maxAge).toString())
                .header(HttpHeaders.SET_COOKIE, cookieCsrf(paquete.csrfToken(), maxAge).toString())
                .body(paquete.respuesta());
    }

    private ResponseCookie cookieRefresh(String valor, Duration maxAge) {
        return ResponseCookie.from(COOKIE_REFRESH, valor)
                .httpOnly(true).secure(cookieSegura).sameSite("Lax")
                .path("/api/v1/autenticacion").maxAge(maxAge).build();
    }

    private ResponseCookie cookieCsrf(String valor, Duration maxAge) {
        return ResponseCookie.from(COOKIE_CSRF, valor)
                .httpOnly(false).secure(cookieSegura).sameSite("Lax")
                .path("/").maxAge(maxAge).build();
    }
}
