package com.minitrello.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP rate limiting for auth endpoints (login/register/refresh) —
 * these are the highest-value brute-force / credential-stuffing targets
 * in the whole system, so they get their own stricter limit rather than
 * relying only on a global limit.
 *
 * Uses an in-memory Bucket4j bucket per IP for now. This is correct for a
 * single backend instance; once we run multiple replicas (see Phase 2
 * deployment diagram), this must move to the Redis-backed Bucket4j
 * extension so limits are shared across instances — tracked as a
 * Phase 10 hardening item, not a Phase 5 blocker.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int requestsPerMinute;

    public RateLimitFilter(@Value("${app.rate-limit.requests-per-minute}") int requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!isRateLimited(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Bucket bucket = buckets.computeIfAbsent(clientIp(request), ip -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many requests. Please try again later.\"}");
        }
    }

    private boolean isRateLimited(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/auth/");
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.classic(requestsPerMinute,
                io.github.bucket4j.Refill.greedy(requestsPerMinute, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : request.getRemoteAddr();
    }
}
