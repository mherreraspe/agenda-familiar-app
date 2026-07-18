package com.obusystem.agendafamiliar.agenda.configuracion;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ConfiguracionSeguridad {
    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> { }))
                .build();
    }

    @Bean
    JwtDecoder decodificadorJwt(@Value("${seguridad.jwt.secreto}") String secreto,
            @Value("${seguridad.jwt.emisor}") String emisor) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe contener al menos 32 bytes");
        }
        SecretKey clave = new SecretKeySpec(bytes, MacAlgorithm.HS256.getName());
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(clave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decodificador.setJwtValidator(org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer(emisor));
        return decodificador;
    }
}
