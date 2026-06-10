package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {

    List<UserConsent> findByUserIdOrderByAcceptedAtDesc(UUID userId);

    Page<UserConsent> findAllByOrderByAcceptedAtDesc(Pageable pageable);
}
