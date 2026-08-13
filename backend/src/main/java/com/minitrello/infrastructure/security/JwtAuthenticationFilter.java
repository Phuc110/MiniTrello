package com.minitrello.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Runs once per request. Reads the Bearer access token, validates it via
 * JwtProvider, and — if valid — loads full user details and populates the
 * SecurityContext so downstream @PreAuthorize checks and controller
 * @AuthenticationPrincipal injection work normally.
 *
 * Deliberately permissive on missing/invalid tokens: it just leaves the
 * SecurityContext empty and lets the request continue. SecurityConfig's
 * authorization rules (not this filter) are what actually reject
 * unauthenticated requests to protected endpoints — keeping "is this
 * token valid" and "is this endpoint protected" as separate concerns.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && jwtProvider.isValid(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            UUID userId = jwtProvider.extractUserId(token);
            UserDetails userDetails = userDetailsService.loadUserById(userId);

            var authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
