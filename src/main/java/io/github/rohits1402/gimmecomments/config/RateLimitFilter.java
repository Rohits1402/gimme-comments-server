package io.github.rohits1402.gimmecomments.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

/**
 * Caps how often one address may hit the account endpoints.
 * <p>
 * These are the endpoints an attacker can reach without an account, and two of them
 * send a real email on every call. Everything else is either behind a token or is a
 * read.
 * <p>
 * Runs immediately after request logging so a refused request still appears in the
 * log with its id, but does no other work — no token parsing, no database.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> GUARDED = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/account-verification/generate-otp",
            "/api/v1/auth/account-verification/verify-account",
            "/api/v1/auth/forget-password/generate-otp",
            "/api/v1/auth/forget-password/verify-otp",
            "/api/v1/auth/forget-password/change-password");

    /** Generous for a person, useless for a script. */
    private static final long REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !GUARDED.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Correct only because forward-headers-strategy is set: without it this is the
        // proxy's address and every caller shares one bucket.
        String caller = request.getRemoteAddr();

        if (!rateLimiter.allow("ip:" + caller, REQUESTS, WINDOW)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
            response.getWriter().write("{\"msg\":\"Too many requests. Wait a minute and try again.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}