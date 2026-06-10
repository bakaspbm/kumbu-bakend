package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.PropertyRentalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PropertyRentalRequestRepository extends JpaRepository<PropertyRentalRequest, UUID> {

    List<PropertyRentalRequest> findByRenterIdOrderByCreatedAtDesc(UUID renterId);

    boolean existsByProductIdAndRenterIdAndStatus(String productId, UUID renterId, String status);

    List<PropertyRentalRequest> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Page<PropertyRentalRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<PropertyRentalRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    long countByStatus(String status);

    @Query("""
            SELECT r FROM PropertyRentalRequest r
            WHERE r.productId = :productId
              AND r.status IN ('pending', 'confirmed')
              AND r.rentalMode = 'daily'
              AND r.checkIn IS NOT NULL AND r.checkOut IS NOT NULL
            """)
    List<PropertyRentalRequest> findOccupiedRanges(@Param("productId") String productId);

    @Query("""
            SELECT COUNT(r) > 0 FROM PropertyRentalRequest r
            WHERE r.productId = :productId
              AND r.status IN ('pending', 'confirmed')
              AND r.rentalMode = 'daily'
              AND r.checkIn < :checkOut AND r.checkOut > :checkIn
            """)
    boolean existsOverlap(@Param("productId") String productId,
                          @Param("checkIn") LocalDate checkIn,
                          @Param("checkOut") LocalDate checkOut);
}
