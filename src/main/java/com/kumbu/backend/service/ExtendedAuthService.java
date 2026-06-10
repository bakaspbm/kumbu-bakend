package com.kumbu.backend.service;

import com.kumbu.backend.config.KumbuProperties;
import com.kumbu.backend.domain.entity.AuthToken;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.enums.SignupAuthMethod;
import com.kumbu.backend.domain.enums.SignupSource;
import com.kumbu.backend.dto.auth.AuthResponse;
import com.kumbu.backend.dto.auth.OAuthPublicConfigResponse;
import com.kumbu.backend.dto.auth.OAuthProfileHint;
import com.kumbu.backend.dto.auth.OAuthRequest;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.AuthTokenRepository;
import com.kumbu.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExtendedAuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final AuthService authService;
    private final NotificationMailService mailService;
    private final OAuthVerifier oauthVerifier;
    private final KumbuProperties properties;

    public OAuthPublicConfigResponse getOAuthPublicConfig() {
        var oauth = properties.getOauth();
        String googleId = blankToNull(oauth.getGoogleClientId());
        String facebookId = blankToNull(oauth.getFacebookAppId());
        return OAuthPublicConfigResponse.builder()
                .googleEnabled(googleId != null)
                .facebookEnabled(facebookId != null)
                .googleClientId(googleId)
                .facebookAppId(facebookId)
                .build();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    @Transactional
    public String forgotPassword(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim()).map(user -> {
            String raw = generateToken(32);
            saveToken(user.getId(), user.getEmail(), null, raw, "password_reset", 3600);
            return mailService.sendPasswordReset(user.getEmail(), raw);
        }).orElse(null);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        AuthToken authToken = findValidToken(token, "password_reset");
        User user = userRepository.findById(authToken.getUserId()).orElseThrow();
        user.setPasswordHash(authService.encodePassword(newPassword));
        authToken.setUsedAt(Instant.now());
        authTokenRepository.save(authToken);
        userRepository.save(user);
    }

    @Transactional
    public String sendEmailVerification(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.isEmailVerified()) return null;
        String raw = generateToken(32);
        authTokenRepository.deleteByUserIdAndTokenType(userId, "email_verify");
        saveToken(userId, user.getEmail(), null, raw, "email_verify", 86400);
        return mailService.sendEmailVerification(user.getEmail(), raw);
    }

    @Transactional
    public String resendEmailVerificationByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email.trim()).map(user -> {
            if (!user.isEmailVerified()) {
                return sendEmailVerification(user.getId());
            }
            return null;
        }).orElse(null);
    }

    @Transactional
    public AuthResponse verifyEmail(String token) {
        AuthToken authToken = findValidToken(token, "email_verify");
        User user = userRepository.findById(authToken.getUserId()).orElseThrow();
        user.setEmailVerified(true);
        authToken.setUsedAt(Instant.now());
        authTokenRepository.save(authToken);
        userRepository.save(user);
        return authService.buildAuthResponseForUser(user);
    }

    @Transactional
    public void sendPhoneOtp(String phone) {
        String normalized = normalizePhone(phone);
        User user = userRepository.findByPhone(normalized)
                .orElseGet(() -> userRepository.save(User.builder()
                        .phone(normalized)
                        .displayName("")
                        .signupSource(SignupSource.APP)
                        .signupAuthMethod(SignupAuthMethod.PHONE)
                        .lastActiveSource(SignupSource.APP)
                        .phoneVerified(false)
                        .build()));

        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        authTokenRepository.deleteByUserIdAndTokenType(user.getId(), "phone_otp");
        saveToken(user.getId(), null, normalized, otp, "phone_otp", 300);
        mailService.sendPhoneOtp(normalized, otp);
    }

    @Transactional
    public AuthResponse verifyPhoneOtp(String phone, String otp) {
        String normalized = normalizePhone(phone);
        AuthToken authToken = authTokenRepository
                .findByTokenHashAndTokenType(AuthService.hashToken(otp), "phone_otp")
                .filter(t -> normalized.equals(t.getPhone()) && t.isValid())
                .orElseThrow(() -> ApiException.unauthorized("Código inválido ou expirado"));

        User user = userRepository.findById(authToken.getUserId()).orElseThrow();
        user.setPhone(normalized);
        user.setPhoneVerified(true);
        user.setSignupAuthMethod(SignupAuthMethod.PHONE);
        authToken.setUsedAt(Instant.now());
        authTokenRepository.save(authToken);
        userRepository.save(user);
        return authService.buildAuthResponseForUser(user);
    }

    @Transactional
    public AuthResponse oauthLogin(String provider, OAuthRequest request, SignupSource source) {
        OAuthVerifier.OAuthUserInfo info = oauthVerifier.verify(
                provider,
                request.getAccessToken(),
                request.getProfile());
        SignupSource activeSource = source != null ? source : SignupSource.WEB;
        SignupAuthMethod authMethod = "google".equalsIgnoreCase(provider)
                ? SignupAuthMethod.GOOGLE
                : SignupAuthMethod.FACEBOOK;

        User user = userRepository.findByEmailIgnoreCase(info.email())
                .map(existing -> mergeOAuthProfile(existing, info, authMethod, activeSource))
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(info.email().toLowerCase())
                        .displayName(info.name())
                        .photoUrl(info.photoUrl())
                        .signupSource(activeSource)
                        .signupAuthMethod(authMethod)
                        .lastActiveSource(activeSource)
                        .emailVerified(true)
                        .build()));

        if (!user.isActive()) throw ApiException.forbidden("Conta suspensa ou eliminada");
        return authService.buildAuthResponseForUser(user);
    }

    private User mergeOAuthProfile(
            User user,
            OAuthVerifier.OAuthUserInfo info,
            SignupAuthMethod authMethod,
            SignupSource source) {
        if (info.name() != null && !info.name().isBlank()
                && (user.getDisplayName() == null || user.getDisplayName().isBlank())) {
            user.setDisplayName(info.name());
        }
        if (info.photoUrl() != null && !info.photoUrl().isBlank()
                && (user.getPhotoUrl() == null || user.getPhotoUrl().isBlank())) {
            user.setPhotoUrl(info.photoUrl());
        }
        user.setEmailVerified(true);
        user.setSignupAuthMethod(authMethod);
        user.setLastActiveSource(source);
        return userRepository.save(user);
    }

    private AuthToken findValidToken(String raw, String type) {
        return authTokenRepository.findByTokenHashAndTokenType(AuthService.hashToken(raw), type)
                .filter(AuthToken::isValid)
                .orElseThrow(() -> ApiException.badRequest("Token inválido ou expirado"));
    }

    private void saveToken(UUID userId, String email, String phone, String raw, String type, int ttlSeconds) {
        authTokenRepository.save(AuthToken.builder()
                .userId(userId)
                .email(email)
                .phone(phone)
                .tokenHash(AuthService.hashToken(raw))
                .tokenType(type)
                .expiresAt(Instant.now().plusSeconds(ttlSeconds))
                .build());
    }

    private static String generateToken(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return java.util.HexFormat.of().formatHex(b);
    }

    static String normalizePhone(String phone) {
        return phone.replaceAll("[^+0-9]", "").trim();
    }
}
