package com.kumbu.backend.dto.chat;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ChatMessageResponse {
    private UUID id;
    private UUID conversationId;
    private UUID senderId;
    private String body;
    private Instant createdAt;
    private Instant readAt;
    private String messageKind;
    private boolean fromSupport;
}
