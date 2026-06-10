package com.kumbu.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_consents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "consent_type", nullable = false)
    private String consentType;

    @Column(name = "accepted_at", nullable = false)
    private Instant acceptedAt;

    @Column(name = "user_agent")
    private String userAgent;

    @PrePersist
    void onCreate() {
        if (acceptedAt == null) {
            acceptedAt = Instant.now();
        }
    }
}
