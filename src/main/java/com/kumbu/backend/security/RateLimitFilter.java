package com.kumbu.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limit por IP em endpoints sensíveis.
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;

    private record LimitRule(int maxPerMinute) {}

    private static final Map<String, LimitRule> RULES = Map.of(
            "auth", new LimitRule(120),
            "guest-support", new LimitRule(30),
            "phone-verify", new LimitRule(20),
            "file-upload", new LimitRule(40),
            "catalog-view", new LimitRule(120)
    );

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return classify(request.getRequestURI(), request.getMethod()) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ruleKey = classify(request.getRequestURI(), request.getMethod());
        if (ruleKey == null) {
            filterChain.doFilter(request, response);
            return;
        }

        LimitRule rule = RULES.get(ruleKey);
        String key = clientKey(request) + ":" + ruleKey;
        Window window = windows.computeIfAbsent(key, k -> new Window());

        synchronized (window) {
            long now = System.currentTimeMillis();
            if (now - window.startMs > WINDOW_MS) {
                window.startMs = now;
                window.count.set(0);
            }
            if (window.count.incrementAndGet() > rule.maxPerMinute()) {
                log.warn("Rate limit exceeded for {} ({})", key, ruleKey);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"code\":\"RATE_LIMIT\",\"message\":\"Demasiados pedidos. Tente novamente.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private static String classify(String path, String method) {
        if (path.startsWith("/api/v1/auth/phone/verify")) {
            return "phone-verify";
        }
        if (path.startsWith("/api/v1/auth/")) {
            return "auth";
        }
        if (path.startsWith("/api/v1/support/guest/")) {
            return "guest-support";
        }
        if (path.startsWith("/api/v1/files/") && "POST".equalsIgnoreCase(method)) {
            return "file-upload";
        }
        if (path.matches("/api/v1/catalog/listings/.+/view") && "POST".equalsIgnoreCase(method)) {
            return "catalog-view";
        }
        return null;
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        return ip;
    }

    private static class Window {
        long startMs = System.currentTimeMillis();
        AtomicInteger count = new AtomicInteger(0);
    }
}
