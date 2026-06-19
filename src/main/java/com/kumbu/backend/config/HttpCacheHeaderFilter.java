package com.kumbu.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Camada edge: indica ao browser/CDN quanto tempo podem guardar respostas públicas.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class HttpCacheHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (HttpMethod.GET.matches(request.getMethod())) {
            applyCacheHeaders(request, response);
        } else if (!response.containsHeader("Cache-Control")) {
            response.setHeader("Cache-Control", "no-store");
        }
        filterChain.doFilter(request, response);
    }

    private void applyCacheHeaders(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        boolean authenticated = hasAuthHeader(request);

        if (path.startsWith("/files/")) {
            response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
            return;
        }

        if (path.startsWith("/api/v1/admin/")) {
            response.setHeader("Cache-Control", "private, no-store");
            return;
        }

        if (path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/support/")
                || path.contains("/my-")
                || path.contains("/favorites")) {
            response.setHeader("Cache-Control", "private, no-store");
            return;
        }

        String visibility = authenticated ? "private" : "public";
        if (authenticated) {
            response.setHeader("Vary", "Authorization");
        }

        if (path.startsWith("/api/v1/platform/")
                || path.equals("/api/v1/catalog/categories")
                || path.matches("/api/v1/catalog/categories/[^/]+/subcategories")) {
            response.setHeader("Cache-Control", visibility + ", max-age=300");
            return;
        }

        if (path.startsWith("/api/v1/catalog/")
                || path.startsWith("/api/v1/recommendations/")
                || path.startsWith("/api/v1/monetization/")
                || path.startsWith("/api/v1/jobs")
                || path.startsWith("/api/v1/reviews/products/")) {
            response.setHeader("Cache-Control", visibility + ", max-age=60");
        }
    }

    private static boolean hasAuthHeader(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        return auth != null && !auth.isBlank();
    }
}
