package com.kumbu.backend.domain.entity;

import com.kumbu.backend.domain.converter.OrderStatusConverter;
import com.kumbu.backend.domain.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "seller_id")
    private UUID sellerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "items_count", nullable = false)
    @Builder.Default
    private int itemsCount = 1;

    @Column(name = "total_label", nullable = false)
    private String totalLabel;

    @Convert(converter = OrderStatusConverter.class)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PROCESSING;

    @Column(name = "show_track", nullable = false)
    @Builder.Default
    private boolean showTrack = true;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
