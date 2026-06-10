package com.kumbu.backend.dto.recommendation;

import com.kumbu.backend.dto.catalog.ListingResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendationItemResponse {
    private ListingResponse listing;
    private String reason;
    private String reasonLabel;
    private int score;
}
