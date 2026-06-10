package com.kumbu.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "monetization_daily_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonetizationDailyMetrics {

    @Id
    @Column(name = "metric_date")
    private LocalDate metricDate;

    private int dau;

    @Column(name = "total_users", nullable = false)
    private int totalUsers;

    @Column(name = "active_listings", nullable = false)
    private int activeListings;

    @Column(name = "chats_today", nullable = false)
    private int chatsToday;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}
