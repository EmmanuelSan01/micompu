package co.edu.unab.micompu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración provisional de Spring Security: la dependencia ya está en
 * el {@code pom.xml}, pero la autenticación por roles (incluido el futuro
 * panel de administración que modificará
 * {@link ConfiguracionReservas}) se integrará en una etapa posterior.
 *
 * <p>Hasta entonces se conserva el comportamiento que tenía la API antes
 * de añadir la dependencia: todos los endpoints siguen abiertos
 * ({@code permitAll}) y el CSRF queda deshabilitado (API REST sin
 * sesiones). Sin esta cadena, la auto-configuración de seguridad
 * bloquearía toda la API con 401 (autenticación) y rechazaría
 * POST/PUT/DELETE con 403 (CSRF).
 */
@Configuration
public class ConfiguracionSeguridad {

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
