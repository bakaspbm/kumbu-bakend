package com.kumbu.backend.controller;

import com.kumbu.backend.dto.auth.AuthResponse;
import com.kumbu.backend.dto.auth.EmailActionResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DevEmailActionSanitizer {

    private final Environment environment;

    public DevEmailActionSanitizer(Environment environment) {
        this.environment = environment;
    }

    public AuthResponse sanitize(AuthResponse response) {
        if (response != null && !allowDevLinks()) {
            response.setEmailActionLink(null);
        }
        return response;
    }

    public EmailActionResponse sanitize(EmailActionResponse response) {
        if (response != null && !allowDevLinks()) {
            response.setEmailActionLink(null);
        }
        return response;
    }

    private boolean allowDevLinks() {
        return !Arrays.asList(environment.getActiveProfiles()).contains("prod");
    }
}
