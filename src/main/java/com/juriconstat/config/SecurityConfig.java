package com.juriconstat.config;

import com.juriconstat.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration Spring Security.
 *
 * Routes publiques  : POST /auth/register, POST /auth/login
 * Routes protégées  : tout le reste (ROLE_USER par défaut)
 * Routes admin      : DELETE /users/** (ROLE_ADMIN uniquement)
 *
 * Session : STATELESS — pas de session HTTP, uniquement JWT.
 *
 * @author Borel
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Désactive CSRF (API stateless)
            .csrf(csrf -> csrf.disable())

            // Pas de session HTTP côté serveur
            .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Règles d'autorisation
            .authorizeHttpRequests(auth -> auth
                // Endpoints publics d'authentification
                .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()

                // Console H2 (profil dev uniquement)
                .requestMatchers("/h2-console/**").permitAll()

                // Swagger / OpenAPI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Suppression d'utilisateurs : admin seulement
                .requestMatchers(HttpMethod.DELETE, "/users/**").hasRole("ADMIN")

                // Tout le reste nécessite une authentification
                .anyRequest().authenticated()
            )

            // Autorise les frames pour la console H2 (développement)
            .headers(headers -> headers.frameOptions(frame -> frame.disable()))

            // Ajoute le filtre JWT avant le filtre d'authentification standard
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Encodeur BCrypt (force 10 rounds) partagé dans tout le contexte Spring.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
