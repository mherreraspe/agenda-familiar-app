package com.obusystem.agendafamiliar.autenticacion.sesion;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.obusystem.agendafamiliar.autenticacion.usuario.RepositorioUsuarios;
import com.obusystem.agendafamiliar.autenticacion.usuario.Usuario;

@Service
public class ServicioSesiones {
    private final RepositorioUsuarios usuarios;
    private final PasswordEncoder claves;
    private final JwtEncoder jwtEncoder;
    private final String emisor;
    private final Duration duracion;

    public ServicioSesiones(
            RepositorioUsuarios usuarios,
            PasswordEncoder claves,
            JwtEncoder jwtEncoder,
            @Value("${seguridad.jwt.emisor}") String emisor,
            @Value("${seguridad.jwt.duracion-minutos}") long duracionMinutos) {
        this.usuarios = usuarios;
        this.claves = claves;
        this.jwtEncoder = jwtEncoder;
        this.emisor = emisor;
        this.duracion = Duration.ofMinutes(duracionMinutos);
    }

    @Transactional(readOnly = true)
    public RespuestaSesion iniciar(SolicitudInicioSesion solicitud) {
        Usuario usuario = usuarios.findByCorreoIgnoreCase(solicitud.correo())
                .filter(encontrado -> "ACTIVO".equals(encontrado.getEstado()))
                .orElseThrow(() -> new BadCredentialsException("Credenciales inválidas"));
        if (!claves.matches(solicitud.clave(), usuario.getClaveHash())) {
            throw new BadCredentialsException("Credenciales inválidas");
        }

        Instant ahora = Instant.now();
        Instant expiraEn = ahora.plus(duracion);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor)
                .issuedAt(ahora)
                .expiresAt(expiraEn)
                .subject(usuario.getIdPublico().toString())
                .claim("correo", usuario.getCorreo())
                .build();
        JwsHeader encabezado = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(encabezado, claims)).getTokenValue();
        return new RespuestaSesion(token, expiraEn, usuario.getIdPublico(), usuario.getCorreo());
    }
}
