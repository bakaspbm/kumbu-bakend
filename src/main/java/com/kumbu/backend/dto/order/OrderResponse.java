package com.kumbu.backend.dto.order;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class OrderResponse {
    private String id;
    private UUID userId;
    private UUID sellerId;
    private String status;
    private String totalLabel;
    private int itemsCount;
    private Instant createdAt;
    private boolean showTrack;
}
