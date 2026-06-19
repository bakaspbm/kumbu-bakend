package com.kumbu.backend.dto.chat;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ConversationResponse {
    private UUID id;
    private String productId;
    private String productTitle;
    private UUID buyerId;
    private UUID sellerId;
    private UUID otherPartyId;
    private String otherPartyName;
    private boolean otherPartyVerified;
    private String lastMessageBody;
    private UUID lastMessageSenderId;
    private Instant lastMessageAt;
    private Instant updatedAt;
    private String dealStatus;
    private int unreadCount;
    private Instant otherPartyLastSeenAt;
    private boolean otherPartyOnline;
}
