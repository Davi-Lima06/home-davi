package br.com.davi.homedavi.shared.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit por IP com token bucket em memória. Responde 429 (com Retry-After) ao estourar.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {
    private static final int TOO_MANY_REQUESTS = 429;
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitProperties properties;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = WebClientIp.resolve(request, false);
        double capacity = properties.getCapacity();
        double refillPerSecond = capacity / Math.max(1, properties.getWindow().toSeconds());

        if (buckets.size() >= properties.getMaxTrackedIps() && !buckets.containsKey(clientIp)) {
            // Fail-open: não deixa o mapa crescer sem limite, mas não derruba tráfego legítimo.
            filterChain.doFilter(request, response);
            return;
        }
        Bucket bucket = buckets.computeIfAbsent(clientIp, ignored -> new Bucket(capacity, System.nanoTime()));
        if (bucket.tryConsume(capacity, refillPerSecond, System.nanoTime())) {
            filterChain.doFilter(request, response);
            return;
        }

        long retryAfter = (long) Math.ceil(1.0 / refillPerSecond);
        response.setStatus(TOO_MANY_REQUESTS);
        response.setHeader("Retry-After", Long.toString(retryAfter));
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write("Rate limit exceeded. Try again in " + retryAfter + "s.");
        log.warn("Rate limit exceeded for IP {}: {} {}", clientIp, request.getMethod(), request.getRequestURI());
    }

    /**
     * Token bucket simples; o acesso é serializado por bucket (um por IP).
     */
    private static final class Bucket {
        private double tokens;
        private long lastRefillNanos;

        Bucket(double tokens, long now) {
            this.tokens = tokens;
            this.lastRefillNanos = now;
        }

        synchronized boolean tryConsume(double capacity, double refillPerSecond, long now) {
            double elapsedSeconds = (now - lastRefillNanos) / 1_000_000_000.0;
            tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
            lastRefillNanos = now;
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }
    }
}
