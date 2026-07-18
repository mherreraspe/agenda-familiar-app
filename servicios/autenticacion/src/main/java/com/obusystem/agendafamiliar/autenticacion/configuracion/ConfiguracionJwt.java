package com.obusystem.agendafamiliar.autenticacion.configuracion;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

@Configuration
public class ConfiguracionJwt {
    @Bean
    SecretKey claveJwt(@Value("${seguridad.jwt.secreto}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe contener al menos 32 bytes");
        }
        return new SecretKeySpec(bytes, MacAlgorithm.HS256.getName());
    }

    @Bean
    JwtEncoder codificadorJwt(SecretKey claveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveJwt));
    }

    @Bean
    PasswordEncoder codificadorClaves() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }
}
