package com.kumbu.backend.service;

import com.kumbu.backend.config.KumbuProperties;
import com.kumbu.backend.domain.entity.AdminUser;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.enums.AdminRole;
import com.kumbu.backend.domain.enums.SignupAuthMethod;
import com.kumbu.backend.domain.enums.SignupSource;
import com.kumbu.backend.repository.AdminUserRepository;
import com.kumbu.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final KumbuProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminUserRepository.count() > 0) {
            return;
        }

        String email = properties.getAdmin().getBootstrapEmail();
        String password = properties.getAdmin().getBootstrapPassword();

        User admin = userRepository.findByEmailIgnoreCase(email).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(email.toLowerCase())
                        .passwordHash(passwordEncoder.encode(password))
                        .displayName("Super Admin")
                        .signupSource(SignupSource.WEB)
                        .signupAuthMethod(SignupAuthMethod.EMAIL)
                        .lastActiveSource(SignupSource.WEB)
                        .emailVerified(true)
                        .build()));

        if (admin.getPasswordHash() == null) {
            admin.setPasswordHash(passwordEncoder.encode(password));
            userRepository.save(admin);
        }

        adminUserRepository.save(AdminUser.builder()
                .user(admin)
                .email(email.toLowerCase())
                .role(AdminRole.SUPER_ADMIN)
                .build());

        log.info("Admin bootstrap criado: {}", email);
    }
}
