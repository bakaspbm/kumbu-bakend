package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.MonetizationPayment;
import com.kumbu.backend.domain.enums.PlatformPaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonetizationPaymentRepository extends JpaRepository<MonetizationPayment, UUID> {

    Page<MonetizationPayment> findByStatusOrderByCreatedAtDesc(PlatformPaymentStatus status, Pageable pageable);

    Page<MonetizationPayment> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<MonetizationPayment> findByStatus(PlatformPaymentStatus status);

    Optional<MonetizationPayment> findByReferenceCode(String referenceCode);

    long countByStatus(PlatformPaymentStatus status);

    @Query("""
            SELECT p FROM MonetizationPayment p
            WHERE p.status IN ('PENDING', 'AWAITING_CONFIRMATION')
              AND p.createdAt < :deadline
            ORDER BY p.createdAt ASC
            """)
    List<MonetizationPayment> findOverdueForSla(@Param("deadline") Instant deadline);

    @Query("""
            SELECT COUNT(p) FROM MonetizationPayment p
            WHERE p.status = 'CONFIRMED'
            """)
    long countConfirmed();

    @Query("""
            SELECT COUNT(p) FROM MonetizationPayment p
            WHERE p.status IN ('CONFIRMED', 'REJECTED', 'EXPIRED')
              AND p.proofUrl IS NOT NULL
            """)
    long countWithProofResolved();

    @Query("""
            SELECT COUNT(p) FROM MonetizationPayment p
            WHERE p.status = 'AWAITING_CONFIRMATION'
            """)
    long countAwaitingConfirmation();

    List<MonetizationPayment> findByStatusAndCreatedAtBefore(PlatformPaymentStatus status, Instant before);
}
