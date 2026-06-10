package com.kumbu.backend.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresInSeconds;
    private UUID userId;
    private String email;
    private String displayName;
    private boolean admin;
    private boolean emailVerified;
    /** Preenchido quando SMTP não enviou — usar em dev para confirmar sem email real */
    private String emailActionLink;
}
