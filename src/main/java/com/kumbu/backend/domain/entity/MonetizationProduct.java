package com.kumbu.backend.domain.entity;

import com.kumbu.backend.domain.enums.MonetizationFeatureType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "monetization_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonetizationProduct {

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false)
    private MonetizationFeatureType featureType;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "price_kz", nullable = false)
    private long priceKz;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Column(name = "max_listings")
    private Integer maxListings;

    @Column(name = "category_hint")
    private String categoryHint;

    @Column(name = "category_id")
    private String categoryId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
