package com.kumbu.backend.controller;



import com.kumbu.backend.domain.enums.SignupSource;

import com.kumbu.backend.dto.auth.AuthResponse;

import com.kumbu.backend.dto.auth.FacebookCodeRequest;
import com.kumbu.backend.dto.auth.EmailActionResponse;

import com.kumbu.backend.dto.auth.EmailRequest;

import com.kumbu.backend.dto.auth.LoginRequest;

import com.kumbu.backend.dto.auth.OAuthPublicConfigResponse;
import com.kumbu.backend.dto.auth.OAuthRequest;

import com.kumbu.backend.dto.auth.PhoneRequest;

import com.kumbu.backend.dto.auth.PhoneVerifyRequest;

import com.kumbu.backend.dto.auth.RefreshRequest;

import com.kumbu.backend.dto.auth.RegisterRequest;

import com.kumbu.backend.dto.auth.ResetPasswordRequest;

import com.kumbu.backend.dto.auth.TokenRequest;

import com.kumbu.backend.security.SecurityUtils;

import com.kumbu.backend.service.AuthService;

import com.kumbu.backend.service.ExtendedAuthService;

import com.kumbu.backend.validation.OneOf;

import jakarta.validation.Valid;

import jakarta.validation.constraints.NotBlank;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



@RestController

@RequestMapping("/api/v1/auth")

@RequiredArgsConstructor

@Validated

public class AuthController {



    private final AuthService authService;

    private final ExtendedAuthService extendedAuthService;

    private final SecurityUtils securityUtils;

    private final DevEmailActionSanitizer devEmailActionSanitizer;



    @PostMapping("/register")

    @ResponseStatus(HttpStatus.CREATED)

    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        String link = extendedAuthService.sendEmailVerification(response.getUserId());

        if (link != null) {

            response.setEmailActionLink(link);

        }

        return devEmailActionSanitizer.sanitize(response);

    }



    @PostMapping("/login")

    public AuthResponse login(@Valid @RequestBody LoginRequest request) {

        return authService.login(request);

    }



    @PostMapping("/logout")

    @ResponseStatus(HttpStatus.NO_CONTENT)

    public void logout(@Valid @RequestBody RefreshRequest request) {

        authService.logout(request.getRefreshToken());

    }



    @PostMapping("/logout-all")

    @ResponseStatus(HttpStatus.NO_CONTENT)

    public void logoutAll() {

        authService.logoutAllSessions(securityUtils.currentUserId());

    }



    @PostMapping("/refresh")

    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {

        return authService.refresh(request.getRefreshToken());

    }



    @PostMapping("/forgot-password")

    public EmailActionResponse forgotPassword(@Valid @RequestBody EmailRequest request) {

        String link = extendedAuthService.forgotPassword(request.getEmail());

        return devEmailActionSanitizer.sanitize(EmailActionResponse.builder()

                .message("Se existir conta com este email, enviámos instruções.")

                .emailActionLink(link)

                .build());

    }



    @PostMapping("/reset-password")

    @ResponseStatus(HttpStatus.NO_CONTENT)

    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {

        extendedAuthService.resetPassword(request.getToken(), request.getPassword());

    }



    @PostMapping("/verify-email")

    public AuthResponse verifyEmail(@Valid @RequestBody TokenRequest request) {

        return extendedAuthService.verifyEmail(request.getToken());

    }



    @PostMapping("/resend-verification")

    public EmailActionResponse resendVerification() {

        String link = extendedAuthService.sendEmailVerification(securityUtils.currentUserId());

        return devEmailActionSanitizer.sanitize(EmailActionResponse.builder()

                .message("Link de confirmação reenviado.")

                .emailActionLink(link)

                .build());

    }



    @PostMapping("/resend-verification-email")

    public EmailActionResponse resendVerificationEmail(@Valid @RequestBody EmailRequest request) {

        String link = extendedAuthService.resendEmailVerificationByEmail(request.getEmail());

        return devEmailActionSanitizer.sanitize(EmailActionResponse.builder()

                .message("Se a conta existir e não estiver confirmada, enviámos um novo link.")

                .emailActionLink(link)

                .build());

    }



    @PostMapping("/phone/send")

    @ResponseStatus(HttpStatus.NO_CONTENT)

    public void sendPhoneOtp(@Valid @RequestBody PhoneRequest request) {

        extendedAuthService.sendPhoneOtp(request.getPhone());

    }



    @PostMapping("/phone/verify")

    public AuthResponse verifyPhoneOtp(@Valid @RequestBody PhoneVerifyRequest request) {

        return extendedAuthService.verifyPhoneOtp(request.getPhone(), request.getToken());

    }



    @GetMapping("/oauth/config")
    public OAuthPublicConfigResponse oauthConfig() {
        return extendedAuthService.getOAuthPublicConfig();
    }

    @PostMapping("/oauth/{provider}")

    public AuthResponse oauth(

            @PathVariable @NotBlank @OneOf(value = {"google", "facebook"}, message = "Provider OAuth inválido") String provider,

            @Valid @RequestBody OAuthRequest request) {

        SignupSource source = SignupSource.valueOf(request.getSignupSource().toUpperCase());

        return extendedAuthService.oauthLogin(provider, request, source);

    }

    @PostMapping("/oauth/facebook/code")
    public AuthResponse facebookCode(@Valid @RequestBody FacebookCodeRequest request) {
        SignupSource source = SignupSource.valueOf(request.getSignupSource().toUpperCase());
        return devEmailActionSanitizer.sanitize(
                extendedAuthService.oauthLoginWithFacebookCode(
                        request.getCode(),
                        request.getRedirectUri(),
                        source));
    }

}

