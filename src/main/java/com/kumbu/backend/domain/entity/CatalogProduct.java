package com.kumbu.backend.domain.entity;

import com.kumbu.backend.domain.converter.ListingKindConverter;
import com.kumbu.backend.domain.enums.ListingKind;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "catalog_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogProduct {

    @Id
    private String id;

    @Column(name = "category_id", nullable = false)
    private String categoryId;

    @Column(name = "subcategory_id")
    private String subcategoryId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal rating = new BigDecimal("4.50");

    @Column(name = "price_label", nullable = false)
    private String priceLabel;

    @Column(name = "old_price_label")
    private String oldPriceLabel;

    @Column(name = "discount_percent")
    private Integer discountPercent;

    @Column(name = "delivery_text")
    private String deliveryText;

    @Column(name = "image_color")
    private Long imageColor;

    @Column(name = "image_url")
    private String imageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "image_urls", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    private String description;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private boolean featured = false;

    @Column(name = "is_out_of_stock", nullable = false)
    @Builder.Default
    private boolean outOfStock = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "seller_id")
    private UUID sellerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", insertable = false, updatable = false)
    private User seller;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Convert(converter = ListingKindConverter.class)
    @Column(name = "listing_kind", nullable = false)
    @Builder.Default
    private ListingKind listingKind = ListingKind.GENERAL;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "property_meta", columnDefinition = "jsonb")
    private Map<String, Object> propertyMeta;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "job_meta", columnDefinition = "jsonb")
    private Map<String, Object> jobMeta;

    @Column(name = "job_listing_status")
    @Builder.Default
    private String jobListingStatus = "active";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "product_meta", columnDefinition = "jsonb")
    private Map<String, Object> productMeta;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private int viewCount = 0;

    @Column(name = "review_count", nullable = false)
    @Builder.Default
    private int reviewCount = 0;

    @Column(name = "featured_until")
    private Instant featuredUntil;

    @Column(name = "highlight_type")
    private String highlightType;

    @Column(name = "boosted_until")
    private Instant boostedUntil;

    @Column(name = "boost_score", nullable = false)
    @Builder.Default
    private int boostScore = 0;

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

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
