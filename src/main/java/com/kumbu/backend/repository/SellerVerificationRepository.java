package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.SellerVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SellerVerificationRepository extends JpaRepository<SellerVerification, UUID> {

    List<SellerVerification> findByStatusOrderByCreatedAtDesc(String status);

    Optional<SellerVerification> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

    Page<SellerVerification> findByTierOrderByCreatedAtDesc(String tier, Pageable pageable);

    Page<SellerVerification> findByTierAndStatusOrderByCreatedAtDesc(String tier, String status, Pageable pageable);
}
