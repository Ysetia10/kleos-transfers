package com.kleos.transfers.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Blocks unauthenticated data-ingest writes on the public API.
 *
 * <p>In production, only {@code POST /api/v1/predictions} stays open for the web app. Bulk import,
 * CRUD mutations, media updates, and evaluation writes require {@code X-Kleos-Ingest-Key} (or
 * {@code Authorization: Bearer}) matching {@code kleos.write-protection.api-key}.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class WriteProtectionFilter extends OncePerRequestFilter {

    static final String INGEST_KEY_HEADER = "X-Kleos-Ingest-Key";
    private static final String PUBLIC_PREDICTIONS_PATH = "/api/v1/predictions";

    private static final Logger log = LoggerFactory.getLogger(WriteProtectionFilter.class);

    private final boolean enabled;
    private final String apiKey;

    public WriteProtectionFilter(
            @Value("${kleos.write-protection.enabled:false}") boolean enabled,
            @Value("${kleos.write-protection.api-key:}") String apiKey
    ) {
        this.enabled = enabled;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String method = request.getMethod();
        if (HttpMethod.GET.matches(method)
                || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method)) {
            return true;
        }
        String path = normalizePath(request.getRequestURI());
        if (path.startsWith("/actuator/") || path.equals("/api/v1/health")) {
            return true;
        }
        if (HttpMethod.POST.matches(method) && path.equals(PUBLIC_PREDICTIONS_PATH)) {
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (apiKey.isEmpty()) {
            log.warn(
                    "Blocked {} {} — write protection enabled but kleos.write-protection.api-key is unset",
                    request.getMethod(),
                    request.getRequestURI()
            );
            writeForbidden(response, "Write access is disabled on this deployment.");
            return;
        }

        String provided = resolveProvidedKey(request);
        if (!apiKey.equals(provided)) {
            log.warn(
                    "Blocked unauthorized {} {} from ip={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    RateLimitFilter.resolveClientIp(request)
            );
            writeForbidden(response, "Valid ingest credentials are required for this operation.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    static String normalizePath(String requestUri) {
        if (requestUri == null || requestUri.isEmpty()) {
            return "/";
        }
        int queryIndex = requestUri.indexOf('?');
        String path = queryIndex >= 0 ? requestUri.substring(0, queryIndex) : requestUri;
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    static String resolveProvidedKey(HttpServletRequest request) {
        String header = request.getHeader(INGEST_KEY_HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return "";
    }

    private static void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"" + escapeJson(message) + "\"}"
        );
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
