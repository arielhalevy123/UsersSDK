package com.example.userssdk.config;

import com.example.userssdk.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())  // uses the corsConfigurationSource bean below
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // User listings: never public. /all is admin-only; /admin/{id}/users needs a
                        // token and the controller checks it is that admin.
                        .requestMatchers(HttpMethod.GET, "/api/auth/all").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/auth/admin/*/users").authenticated()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/favicon.ico").permitAll()
                        .requestMatchers("/api/auth/my-users").permitAll()
                        .requestMatchers("/style.css", "/script.js", "/index.html").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/auth/users/**").permitAll()
                        // OpenAPI docs. Browsable API description only — it exposes no data and
                        // every documented endpoint still enforces its own authentication.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS for browser clients on other origins (configure with CORS_ALLOWED_ORIGINS).
     * The Admin Portal is same-origin and the Android SDK is not a browser, so neither needs it.
     * Auth uses a bearer token, not cookies, so credentials are not allowed cross-origin.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
        CorsConfiguration cfg = new CorsConfiguration();
        allowedOrigins.stream().map(String::trim).filter(o -> !o.isEmpty())
                .forEach(cfg::addAllowedOriginPattern);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}
