package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    Optional<AuthToken> findByTokenHashAndTokenType(String tokenHash, String tokenType);

    void deleteByUserIdAndTokenType(UUID userId, String tokenType);
}
