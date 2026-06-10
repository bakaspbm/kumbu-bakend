package com.kumbu.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "monetization_category_strategies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonetizationCategoryStrategy {

    @Id
    @Column(name = "category_id")
    private String categoryId;

    @Column(name = "primary_monetization", nullable = false)
    private String primaryMonetization;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "secondary_monetizations", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> secondaryMonetizations = new ArrayList<>();

    @Column(name = "strategy_title", nullable = false)
    private String strategyTitle;

    @Column(name = "strategy_description")
    private String strategyDescription;

    @Column(name = "why_description")
    private String whyDescription;

    @Column(name = "cta_message")
    private String ctaMessage;

    @Column(name = "cta_button_label")
    private String ctaButtonLabel;

    @Column(name = "revenue_tier", nullable = false)
    @Builder.Default
    private String revenueTier = "MEDIUM";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enabled_feature_types", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> enabledFeatureTypes = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
