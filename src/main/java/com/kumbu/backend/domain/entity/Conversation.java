package com.kumbu.backend.domain.entity;

import com.kumbu.backend.domain.converter.ConversationTypeConverter;
import com.kumbu.backend.domain.converter.DealStatusConverter;
import com.kumbu.backend.domain.converter.SupportStatusConverter;
import com.kumbu.backend.domain.enums.ConversationType;
import com.kumbu.backend.domain.enums.DealStatus;
import com.kumbu.backend.domain.enums.SupportStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "buyer_id")
    private UUID buyerId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "is_blocked", nullable = false)
    @Builder.Default
    private boolean blocked = false;

    @Column(name = "blocked_reason")
    private String blockedReason;

    @Column(name = "blocked_at")
    private Instant blockedAt;

    @Column(name = "blocked_by")
    private UUID blockedBy;

    @Convert(converter = DealStatusConverter.class)
    @Column(name = "deal_status", nullable = false)
    @Builder.Default
    private DealStatus dealStatus = DealStatus.OPEN;

    @Column(name = "deal_status_at")
    private Instant dealStatusAt;

    @Column(name = "deal_status_by")
    private UUID dealStatusBy;

    @Convert(converter = ConversationTypeConverter.class)
    @Column(name = "conversation_type", nullable = false)
    @Builder.Default
    private ConversationType conversationType = ConversationType.MARKETPLACE;

    @Convert(converter = SupportStatusConverter.class)
    @Column(name = "support_status")
    private SupportStatus supportStatus;

    @Column(name = "guest_name")
    private String guestName;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(name = "guest_access_token")
    private String guestAccessToken;

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
