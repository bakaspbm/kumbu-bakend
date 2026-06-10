package com.kumbu.backend.dto.monetization;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class AdminCreateMonetizationProductRequest {

    private String id;
    private String featureType;
    private String name;
    private String description;
    private Long priceKz;
    private Integer durationDays;
    private Integer maxListings;
    private String categoryHint;
    private String categoryId;
    private Boolean active;
    private Integer sortOrder;
    private Map<String, Object> metadata;
}
