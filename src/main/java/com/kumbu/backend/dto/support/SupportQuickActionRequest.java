package com.kumbu.backend.dto.support;

import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SupportQuickActionRequest {
    @NotBlank
    private String actionId;
}
