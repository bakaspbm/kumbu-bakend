package com.kumbu.backend.dto.catalog;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ListingResponse {
    private String id;
    private String title;
    private String priceLabel;
    private BigDecimal price;
    private String city;
    private String region;
    private List<String> imageUrls;
    private String description;
    private String categoryId;
    private String subcategoryId;
    private String listingKind;
    private BigDecimal rating;
    private int reviewCount;
    private int viewCount;
    private boolean featured;
    private boolean outOfStock;
    private UUID sellerId;
    private String sellerName;
    private String sellerPhotoUrl;
    private Instant createdAt;
    private Object propertyMeta;
    private Object jobMeta;
    private Object productMeta;
    private boolean favorite;
}
