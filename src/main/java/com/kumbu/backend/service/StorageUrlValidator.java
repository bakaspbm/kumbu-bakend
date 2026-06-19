package com.kumbu.backend.service;

import com.kumbu.backend.config.KumbuProperties;
import com.kumbu.backend.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StorageUrlValidator {

    private final KumbuProperties properties;
    private final StorageService storageService;

    public void assertOwnedChatOrListingUrl(UUID userId, String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String relative = toRelativePublicPath(url);
        if (relative == null) {
            throw ApiException.badRequest("URL de anexo inválida");
        }
        if (relative.startsWith("chat/")) {
            String prefix = "chat/" + userId + "_";
            if (!relative.startsWith(prefix)) {
                throw ApiException.badRequest("Anexo inválido para esta conta");
            }
            return;
        }
        if (relative.startsWith("listings/")) {
            String prefix = "listings/" + userId + "_";
            if (!relative.startsWith(prefix)) {
                throw ApiException.badRequest("Anexo inválido para esta conta");
            }
            return;
        }
        throw ApiException.badRequest("URL de anexo não permitida");
    }

    public String toRelativePublicPath(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String trimmed = url.trim();
        String publicBase = properties.getStorage().getPublicBaseUrl();
        if (publicBase != null && !publicBase.isBlank() && trimmed.startsWith(publicBase)) {
            return trimmed.substring(publicBase.length()).replaceFirst("^/+", "");
        }
        try {
            URI uri = URI.create(trimmed);
            String path = uri.getPath();
            if (path == null) {
                return null;
            }
            if (path.startsWith("/files/")) {
                return path.substring("/files/".length());
            }
            if (path.startsWith("files/")) {
                return path.substring("files/".length());
            }
        } catch (Exception ignored) {
            /* fall through */
        }
        return null;
    }

    public boolean isAllowedHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
                return false;
            }
            return toRelativePublicPath(url) != null;
        } catch (Exception ex) {
            return false;
        }
    }
}
