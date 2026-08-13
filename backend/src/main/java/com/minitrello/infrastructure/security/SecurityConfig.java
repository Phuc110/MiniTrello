package com.minitrello.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Central security wiring.
 *
 * - Stateless sessions: JWT carries identity, so there is no server-side
 *   HTTP session at all (a prerequisite for the horizontally-scalable
 *   stateless backend from the Phase 2 deployment diagram).
 * - CSRF is disabled: CSRF protection exists to defend session-cookie-
 *   based auth. We don't use cookies for the access token (memory-only,
 *   sent via Authorization header), and the refresh-token cookie is
 *   SameSite=Strict + httpOnly, which already blocks cross-site
 *   submission — see the CSRF explanation note below.
 * - Method security (@PreAuthorize) is enabled here so resource-level
 *   checks (Phase 6+) can live directly on service methods.
 *
 * CSRF note (per project security requirements — "CSRF explanation"):
 * CSRF exploits the browser's automatic inclusion of cookies on
 * cross-site requests. Because our refresh-token cookie is
 * SameSite=Strict, the browser will not attach it to any cross-site
 * request in the first place, which removes the attack vector CSRF
 * tokens exist to close. If SameSite protection were ever relaxed
 * (e.g. to support a cross-subdomain setup), a CSRF token would need
 * to be reintroduced for the /auth/refresh endpoint specifically.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/swagger-ui/**",
            "/api-docs/**",
            "/actuator/health"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers("/ws/**").authenticated()
                        .anyRequest().authenticated())
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, RateLimitFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12: deliberately above the BCrypt default (10) — hashing
        // cost should track hardware improvements over the app's lifetime.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }
}
