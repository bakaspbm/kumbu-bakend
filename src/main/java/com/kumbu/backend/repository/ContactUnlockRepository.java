package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.ContactUnlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContactUnlockRepository extends JpaRepository<ContactUnlock, UUID> {

    boolean existsByBuyerIdAndSellerIdAndProductId(UUID buyerId, UUID sellerId, String productId);

    Optional<ContactUnlock> findByBuyerIdAndSellerIdAndProductId(UUID buyerId, UUID sellerId, String productId);
}
