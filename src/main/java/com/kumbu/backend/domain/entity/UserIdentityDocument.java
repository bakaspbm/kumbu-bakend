package com.kumbu.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_identity_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(UserIdentityDocument.UserIdentityDocumentId.class)
public class UserIdentityDocument {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(nullable = false, length = 16)
    private String side;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "review_status", nullable = false, length = 16)
    @Builder.Default
    private String reviewStatus = "PENDING";

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @PrePersist
    void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = Instant.now();
        }
        if (reviewStatus == null) {
            reviewStatus = "PENDING";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserIdentityDocumentId implements Serializable {
        private UUID userId;
        private String side;
    }
}
