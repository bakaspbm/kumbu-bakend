package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    Optional<AdminUser> findByUserId(UUID userId);

    boolean existsByEmailIgnoreCase(String email);

    Page<AdminUser> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
