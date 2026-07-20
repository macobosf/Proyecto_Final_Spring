package ec.edu.ups.icc.academiceventsapi.auth.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Protege Swagger UI y el JSON de OpenAPI solo en producción, con
 * credenciales de evaluación independientes del login JWT de la API.
 */
@Configuration
@Profile("prod")
public class SwaggerSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http,
                                                            PasswordEncoder passwordEncoder,
                                                            @Value("${app.swagger.username}") String username,
                                                            @Value("${app.swagger.password}") String password) throws Exception {
        InMemoryUserDetailsManager swaggerUsers = new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password(passwordEncoder.encode(password))
                        .roles("SWAGGER")
                        .build());

        http
                .securityMatcher("/swagger-ui/**", "/v3/api-docs/**")
                .userDetailsService(swaggerUsers)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(httpBasic -> {});

        return http.build();
    }
}
