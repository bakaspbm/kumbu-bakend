package com.kumbu.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "property_rental_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyRentalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "renter_id", nullable = false)
    private UUID renterId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "rental_mode", nullable = false)
    private String rentalMode;

    @Column(name = "check_in")
    private LocalDate checkIn;

    @Column(name = "check_out")
    private LocalDate checkOut;

    private Integer nights;

    @Column(name = "guest_message")
    private String guestMessage;

    @Column(nullable = false)
    @Builder.Default
    private String status = "pending";

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "price_snapshot")
    private String priceSnapshot;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
