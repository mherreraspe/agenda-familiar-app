package com.obusystem.agendafamiliar.autenticacion.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class ConfiguracionSeguridad {
    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(autorizacion -> autorizacion
                        .requestMatchers("/actuator/health", "/actuator/info", "/iniciar-sesion").permitAll()
                        .anyRequest().authenticated())
                .build();
    }
}
