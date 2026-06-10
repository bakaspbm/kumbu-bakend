package com.kumbu.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_deletion_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDeletionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private String email;

    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;

    @Column(nullable = false)
    @Builder.Default
    private String source = "app";

    @PrePersist
    void onCreate() {
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
    }
}
