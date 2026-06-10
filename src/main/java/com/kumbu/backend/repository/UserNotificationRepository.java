package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    List<UserNotification> findByUserIdAndHiddenAtIsNullOrderByCreatedAtDesc(UUID userId);

    Page<UserNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadAtIsNullAndHiddenAtIsNull(UUID userId);

    long countByReadAtIsNullAndHiddenAtIsNull();
}
