package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderItemRepository extends JpaRepository<OrderItem, UUID> {

    boolean existsByOrderIdAndProductId(String orderId, String productId);
}
