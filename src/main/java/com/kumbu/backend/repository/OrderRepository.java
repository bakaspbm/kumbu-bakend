package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.Order;
import com.kumbu.backend.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, String> {

    List<Order> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Order> findBySellerIdOrderByCreatedAtDesc(UUID sellerId);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByCreatedAtAfter(Instant since);

    List<Order> findByCreatedAtAfterOrderByCreatedAtAsc(Instant since);

    Page<Order> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Order> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    Optional<Order> findByIdAndUserId(String id, UUID userId);

    Optional<Order> findByIdAndSellerId(String id, UUID sellerId);
}
