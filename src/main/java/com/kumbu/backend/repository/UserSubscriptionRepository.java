package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.UserSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, UUID> {

    List<UserSubscription> findByUserIdAndStatus(UUID userId, String status);

    Optional<UserSubscription> findFirstByUserIdAndStatusOrderByEndsAtDesc(UUID userId, String status);
}
