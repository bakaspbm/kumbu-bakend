package com.kumbu.backend.domain.entity;

import com.kumbu.backend.domain.enums.MonetizationFeatureType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monetization_features")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonetizationFeature {

    @Id
    private String id;

    @Column(name = "phase_id", nullable = false)
    private String phaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false)
    private MonetizationFeatureType featureType;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = false;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "activated_by")
    private UUID activatedBy;

    @Column(name = "requires_approval", nullable = false)
    @Builder.Default
    private boolean requiresApproval = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
