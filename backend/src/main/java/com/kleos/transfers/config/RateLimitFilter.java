package com.kleos.transfers.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Simple per-IP sliding-window rate limit for the public API.
 *
 * <p>Health and actuator probes are excluded so Render keep-warm and uptime checks stay reliable.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final long CLEANUP_INTERVAL_MS = Duration.ofMinutes(5).toMillis();

    private final boolean enabled;
    private final int requestsPerMinute;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private volatile long lastCleanupAt = System.currentTimeMillis();

    public RateLimitFilter(
            @Value("${kleos.rate-limit.enabled:true}") boolean enabled,
            @Value("${kleos.rate-limit.requests-per-minute:60}") int requestsPerMinute
    ) {
        this.enabled = enabled;
        this.requestsPerMinute = Math.max(1, requestsPerMinute);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        return path.startsWith("/actuator/")
                || path.equals("/api/v1/health")
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        maybeCleanup();
        String clientIp = resolveClientIp(request);
        WindowCounter counter = counters.computeIfAbsent(clientIp, ignored -> new WindowCounter());
        int count = counter.incrementAndGet(System.currentTimeMillis());

        response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, requestsPerMinute - count)));

        if (count > requestsPerMinute) {
            log.warn("Rate limit exceeded for ip={} path={} count={}", clientIp, request.getRequestURI(), count);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\","
                            + "\"message\":\"Rate limit exceeded. Try again shortly.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private void maybeCleanup() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupAt < CLEANUP_INTERVAL_MS) {
            return;
        }
        lastCleanupAt = now;
        long cutoff = now - WINDOW.toMillis() * 2;
        Iterator<Map.Entry<String, WindowCounter>> iterator = counters.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, WindowCounter> entry = iterator.next();
            if (entry.getValue().isStale(cutoff)) {
                iterator.remove();
            }
        }
    }

    private static final class WindowCounter {
        private long windowStartMs = System.currentTimeMillis();
        private final AtomicInteger count = new AtomicInteger(0);

        synchronized int incrementAndGet(long nowMs) {
            if (nowMs - windowStartMs >= WINDOW.toMillis()) {
                windowStartMs = nowMs;
                count.set(0);
            }
            return count.incrementAndGet();
        }

        synchronized boolean isStale(long cutoffMs) {
            return windowStartMs < cutoffMs;
        }
    }
}
