package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.ListingPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ListingPromotionRepository extends JpaRepository<ListingPromotion, UUID> {

    List<ListingPromotion> findByProductIdAndActiveTrue(String productId);

    @Query("""
            SELECT lp FROM ListingPromotion lp
            WHERE lp.active = true AND (lp.endsAt IS NULL OR lp.endsAt > :now)
            """)
    List<ListingPromotion> findAllActive(Instant now);
}
