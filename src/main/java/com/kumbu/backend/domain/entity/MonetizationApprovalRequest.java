package com.kumbu.backend.domain.entity;

import com.kumbu.backend.domain.enums.ApprovalRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "monetization_approval_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonetizationApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "request_type", nullable = false)
    private String requestType;

    @Column(name = "phase_id")
    private String phaseId;

    @Column(name = "feature_id")
    private String featureId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApprovalRequestStatus status = ApprovalRequestStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics_snapshot", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> metricsSnapshot = new HashMap<>();

    private String message;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "review_note")
    private String reviewNote;

    @PrePersist
    void onCreate() {
        if (requestedAt == null) requestedAt = Instant.now();
    }
}
