package com.kumbu.backend.dto.monetization;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AdminUpdateCategoryStrategyRequest {

    private String primaryMonetization;
    private List<String> secondaryMonetizations;
    private String strategyTitle;
    private String strategyDescription;
    private String whyDescription;
    private String ctaMessage;
    private String ctaButtonLabel;
    private String revenueTier;
    private List<String> enabledFeatureTypes;
    private Boolean active;
    private Integer sortOrder;
    private Map<String, Object> metadata;
}
