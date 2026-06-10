package com.kumbu.backend.service;

import com.kumbu.backend.config.KumbuProperties;
import com.kumbu.backend.domain.entity.AdminUser;
import com.kumbu.backend.domain.entity.RefreshToken;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.enums.AdminRole;
import com.kumbu.backend.domain.enums.SignupAuthMethod;
import com.kumbu.backend.domain.enums.SignupSource;
import com.kumbu.backend.dto.auth.AuthResponse;
import com.kumbu.backend.dto.auth.LoginRequest;
import com.kumbu.backend.dto.auth.RegisterRequest;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.AdminUserRepository;
import com.kumbu.backend.repository.RefreshTokenRepository;
import com.kumbu.backend.repository.UserRepository;
import com.kumbu.backend.security.JwtService;
import com.kumbu.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final KumbuProperties properties;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw ApiException.conflict("Email já registado");
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getFullName().trim())
                .phone(request.getPhone())
                .signupSource(request.getSignupSource() != null ? request.getSignupSource() : SignupSource.APP)
                .signupAuthMethod(SignupAuthMethod.EMAIL)
                .lastActiveSource(request.getSignupSource() != null ? request.getSignupSource() : SignupSource.APP)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail().toLowerCase().trim(), request.getPassword()));
        } catch (AuthenticationException ex) {
            throw ApiException.unauthorized("Email ou palavra-passe incorrectos");
        }

        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> ApiException.unauthorized("Credenciais inválidas"));

        if (!user.isActive()) {
            throw ApiException.forbidden("Conta suspensa ou eliminada");
        }

        clearExpiredBan(user);
        user.setLastActiveSource(SignupSource.APP);
        user.setLastSeenAt(Instant.now());
        userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.unauthorized("Refresh token inválido"));

        if (!token.isValid()) {
            throw ApiException.unauthorized("Refresh token expirado");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> ApiException.unauthorized("Utilizador não encontrado"));

        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        return buildAuthResponse(user);
    }

    public AuthResponse buildAuthResponseForUser(User user) {
        return buildAuthResponse(user);
    }

    public String encodePassword(String raw) {
        return passwordEncoder.encode(raw);
    }

    private AuthResponse buildAuthResponse(User user) {
        boolean admin = adminUserRepository.findByUserId(user.getId()).isPresent();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), admin);
        String refreshToken = createRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(properties.getJwt().getAccessTokenMinutes() * 60)
                .userId(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .admin(admin)
                .emailVerified(user.isEmailVerified())
                .build();
    }

    private String createRefreshToken(UUID userId) {
        String raw = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .tokenHash(hashToken(raw))
                .expiresAt(Instant.now().plusSeconds(properties.getJwt().getRefreshTokenDays() * 86400))
                .build();
        refreshTokenRepository.save(entity);
        return raw;
    }

    private void clearExpiredBan(User user) {
        if (user.getBannedAt() != null && user.getBannedUntil() != null
                && user.getBannedUntil().isBefore(Instant.now())) {
            user.setBannedAt(null);
            user.setBannedUntil(null);
            user.setBanReason(null);
            user.setBannedBy(null);
        }
    }

    static String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public void revokeRefreshToken(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }
}
