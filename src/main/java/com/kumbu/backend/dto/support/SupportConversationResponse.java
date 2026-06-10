package com.kumbu.backend.dto.support;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SupportConversationResponse {
    private UUID id;
    private String supportStatus;
    private String welcomeMessage;
    private List<Map<String, Object>> quickActions;
    private Instant updatedAt;
}
