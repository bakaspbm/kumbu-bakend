package com.kumbu.backend.dto.support;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class GuestSupportSessionResponse {
    private UUID id;
    private String accessToken;
    private String guestName;
    private String guestEmail;
    private String supportStatus;
    private String welcomeMessage;
    private List<Map<String, Object>> quickActions;
    private Instant updatedAt;
}
