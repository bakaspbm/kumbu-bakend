package com.kumbu.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "monetization_phases")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonetizationPhase {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(name = "min_users", nullable = false)
    private int minUsers;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
