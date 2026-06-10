package com.kumbu.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "job_applications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_id", nullable = false)
    private String jobId;

    @Column(name = "applicant_id", nullable = false)
    private UUID applicantId;

    @Column(name = "employer_id", nullable = false)
    private UUID employerId;

    @Column(name = "cv_id")
    private UUID cvId;

    @Column(nullable = false)
    @Builder.Default
    private String status = "pending";

    @Column(name = "cover_message")
    private String coverMessage;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cv_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> cvSnapshot;

    @Column(name = "cv_viewed_at")
    private Instant cvViewedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
