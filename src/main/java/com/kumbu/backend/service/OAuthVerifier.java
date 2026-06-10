package com.kumbu.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumbu.backend.config.KumbuProperties;
import com.kumbu.backend.dto.auth.OAuthProfileHint;
import com.kumbu.backend.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@Slf4j
public class OAuthVerifier {

    private static final String GRAPH_VERSION = "v21.0";
    private static final Duration GRAPH_TIMEOUT = Duration.ofSeconds(8);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(GRAPH_TIMEOUT)
            .build();
    private final KumbuProperties properties;

    public OAuthVerifier(KumbuProperties properties) {
        this.properties = properties;
    }

    public OAuthUserInfo verify(String provider, String token, OAuthProfileHint profileHint) {
        return switch (provider.toLowerCase()) {
            case "google" -> verifyGoogle(token, profileHint);
            case "facebook" -> verifyFacebook(token, profileHint);
            default -> throw ApiException.badRequest("Provider OAuth não suportado");
        };
    }

    private OAuthUserInfo verifyGoogle(String idToken, OAuthProfileHint profileHint) {
        String token = idToken == null ? "" : idToken.trim();
        if (token.isEmpty()) {
            throw ApiException.badRequest("Token Google em falta");
        }

        try {
            return fetchGoogleProfileFromTokenInfo(token);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            if (isGraphConnectivityFailure(e) && properties.getOauth().isFacebookTrustClientProfile()) {
                OAuthUserInfo trusted = profileFromGoogleHint(token, profileHint);
                if (trusted != null) {
                    log.warn("[OAuth] Google tokeninfo indisponível — a usar perfil do id_token para {}", trusted.email());
                    return trusted;
                }
            }
            if (isGraphConnectivityFailure(e)) {
                log.warn("[OAuth] Google tokeninfo unreachable: {}", e.getMessage());
                throw ApiException.unauthorized(
                        "O servidor não consegue contactar o Google (rede/firewall). "
                                + "Active trust-client-profile em dev ou libere oauth2.googleapis.com:443.");
            }
            log.warn("[OAuth] Google verification failed: {}", e.getMessage());
            throw ApiException.unauthorized("Falha na verificação Google");
        }
    }

    private OAuthUserInfo fetchGoogleProfileFromTokenInfo(String token) throws Exception {
        String encoded = URLEncoder.encode(token, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/tokeninfo?id_token=" + encoded))
                .timeout(GRAPH_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            throw ApiException.unauthorized("Token Google inválido");
        }
        JsonNode j = objectMapper.readTree(res.body());
        validateGoogleAudience(j.path("aud").asText(null));
        String email = j.path("email").asText(null);
        if (email == null || email.isBlank()) {
            throw ApiException.badRequest("Email não disponível na conta Google");
        }
        return new OAuthUserInfo(
                email,
                j.path("name").asText(""),
                j.path("picture").asText(null)
        );
    }

    private OAuthUserInfo profileFromGoogleHint(String idToken, OAuthProfileHint hint) {
        if (hint != null) {
            String email = hint.getEmail() == null ? "" : hint.getEmail().trim();
            String sub = hint.getGoogleSub() == null ? "" : hint.getGoogleSub().trim();
            if (!email.isBlank() && email.contains("@") && !sub.isBlank()) {
                validateGoogleAudienceFromJwt(idToken);
                String name = hint.getName() == null ? "" : hint.getName().trim();
                String photoUrl = hint.getPhotoUrl() == null || hint.getPhotoUrl().isBlank() ? null : hint.getPhotoUrl().trim();
                return new OAuthUserInfo(email, name, photoUrl);
            }
        }
        return decodeGoogleJwtPayload(idToken);
    }

    private OAuthUserInfo decodeGoogleJwtPayload(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                return null;
            }
            byte[] decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
            JsonNode j = objectMapper.readTree(decoded);
            validateGoogleAudience(j.path("aud").asText(null));
            String email = j.path("email").asText(null);
            if (email == null || email.isBlank()) {
                return null;
            }
            return new OAuthUserInfo(
                    email,
                    j.path("name").asText(""),
                    j.path("picture").asText(null)
            );
        } catch (Exception e) {
            log.debug("[OAuth] JWT decode failed: {}", e.getMessage());
            return null;
        }
    }

    private void validateGoogleAudienceFromJwt(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                return;
            }
            byte[] decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
            JsonNode j = objectMapper.readTree(decoded);
            validateGoogleAudience(j.path("aud").asText(null));
        } catch (Exception ignored) {
            /* ignore */
        }
    }

    private void validateGoogleAudience(String aud) {
        String expected = properties.getOauth().getGoogleClientId();
        if (expected == null || expected.isBlank()) {
            return;
        }
        if (aud == null || !expected.equals(aud)) {
            throw ApiException.unauthorized("Token Google inválido para esta aplicação");
        }
    }

    private OAuthUserInfo verifyFacebook(String accessToken, OAuthProfileHint profileHint) {
        String token = accessToken == null ? "" : accessToken.trim();
        if (token.isEmpty()) {
            throw ApiException.badRequest("Token Facebook em falta");
        }

        try {
            return fetchFacebookProfileFromGraph(token);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            if (isGraphConnectivityFailure(e) && properties.getOauth().isFacebookTrustClientProfile()) {
                OAuthUserInfo trusted = profileFromClientHint(profileHint);
                if (trusted != null) {
                    log.warn("[OAuth] Facebook Graph indisponível — a usar perfil validado no browser para {}", trusted.email());
                    return trusted;
                }
            }
            if (isGraphConnectivityFailure(e)) {
                log.warn("[OAuth] Facebook Graph unreachable: {}", e.getMessage());
                throw ApiException.unauthorized(
                        "O servidor não consegue contactar o Facebook (rede/firewall). "
                                + "Active KUMBU_FACEBOOK_TRUST_CLIENT_PROFILE=true em dev ou libere graph.facebook.com:443.");
            }
            log.warn("[OAuth] Facebook verification failed: {}", e.toString());
            throw ApiException.unauthorized("Falha na verificação Facebook: " + e.getMessage());
        }
    }

    private OAuthUserInfo fetchFacebookProfileFromGraph(String token) throws Exception {
        String fields = "id,name,email,picture";
        String url = "https://graph.facebook.com/" + GRAPH_VERSION + "/me?"
                + "fields=" + URLEncoder.encode(fields, StandardCharsets.UTF_8)
                + "&access_token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(GRAPH_TIMEOUT)
                .GET()
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (res.statusCode() != 200) {
            String message = extractGraphErrorMessage(res.body());
            log.warn("[OAuth] Facebook /me failed {}: {}", res.statusCode(), message);
            throw ApiException.unauthorized(message != null ? message : "Token Facebook inválido");
        }

        JsonNode j = objectMapper.readTree(res.body());
        if (j.has("error")) {
            String message = extractGraphErrorMessage(res.body());
            throw ApiException.unauthorized(message != null ? message : "Token Facebook inválido");
        }

        String email = j.path("email").asText(null);
        if (email == null || email.isBlank()) {
            throw ApiException.badRequest(
                    "Email não disponível no Facebook. Autorize o acesso ao email na app Meta.");
        }

        String photoUrl = j.path("picture").path("data").path("url").asText(null);
        return new OAuthUserInfo(email, j.path("name").asText(""), photoUrl);
    }

    private OAuthUserInfo profileFromClientHint(OAuthProfileHint hint) {
        if (hint == null) {
            return null;
        }
        String email = hint.getEmail() == null ? "" : hint.getEmail().trim();
        String facebookId = hint.getFacebookId() == null ? "" : hint.getFacebookId().trim();
        if (email.isBlank() || !email.contains("@") || facebookId.isBlank() || !facebookId.matches("\\d+")) {
            return null;
        }
        String name = hint.getName() == null ? "" : hint.getName().trim();
        String photoUrl = hint.getPhotoUrl() == null || hint.getPhotoUrl().isBlank() ? null : hint.getPhotoUrl().trim();
        return new OAuthUserInfo(email, name, photoUrl);
    }

    private static boolean isGraphConnectivityFailure(Throwable e) {
        Throwable current = e;
        while (current != null) {
            if (current instanceof HttpConnectTimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof java.net.ConnectException) {
                return true;
            }
            String msg = current.getMessage();
            if (msg != null && msg.matches("(?i).*(connect timed out|timed out|connection refused|no route).*")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String extractGraphErrorMessage(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            JsonNode error = objectMapper.readTree(body).path("error");
            if (error.isMissingNode()) {
                return null;
            }
            String message = error.path("message").asText(null);
            if (message != null && !message.isBlank()) {
                return message;
            }
        } catch (Exception ignored) {
            /* ignore */
        }
        return null;
    }

    public record OAuthUserInfo(String email, String name, String photoUrl) {}
}
