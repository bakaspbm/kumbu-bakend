package com.kumbu.backend.config;

import com.kumbu.backend.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Impede arranque em produção com segredos ou flags inseguras por defeito.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class ProductionSecurityValidator implements ApplicationRunner {

    static final String DEFAULT_JWT_SECRET =
            "change-me-in-production-use-at-least-256-bits-secret-key-here";
    static final String DEFAULT_ADMIN_PASSWORD = "Admin123!";

    private final KumbuProperties properties;
    private final AdminUserRepository adminUserRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<String> errors = new ArrayList<>();

        String jwt = properties.getJwt().getSecret();
        if (jwt == null || jwt.isBlank()) {
            errors.add("KUMBU_JWT_SECRET em falta");
        } else if (DEFAULT_JWT_SECRET.equals(jwt)) {
            errors.add("KUMBU_JWT_SECRET ainda é o valor por defeito");
        } else if (jwt.getBytes(StandardCharsets.UTF_8).length < 32) {
            errors.add("KUMBU_JWT_SECRET demasiado curto (mínimo 32 bytes)");
        }

        String adminPassword = properties.getAdmin().getBootstrapPassword();
        if (DEFAULT_ADMIN_PASSWORD.equals(adminPassword) && adminUserRepository.count() == 0) {
            errors.add("KUMBU_ADMIN_PASSWORD não pode ser Admin123! ao criar o primeiro admin");
        }

        if (properties.getOauth().isFacebookTrustClientProfile()) {
            errors.add("KUMBU_FACEBOOK_TRUST_CLIENT_PROFILE deve ser false em produção");
        }

        List<String> corsOrigins = properties.getCors().getAllowedOrigins();
        if (corsOrigins == null || corsOrigins.isEmpty()) {
            errors.add("KUMBU_CORS_ORIGINS em falta");
        } else         if (corsOrigins.stream().anyMatch(o -> o.contains("localhost") || o.contains("127.0.0.1"))) {
            errors.add("KUMBU_CORS_ORIGINS não pode incluir localhost em produção");
        }

        if ("log".equalsIgnoreCase(properties.getSms().getProvider())) {
            errors.add("KUMBU_SMS_PROVIDER não pode ser log em produção");
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "Configuração insegura para produção: " + String.join("; ", errors));
        }
        log.info("Validação de segurança de produção concluída");
    }
}
