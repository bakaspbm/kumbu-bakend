package com.kumbu.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "monetization_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonetizationSettings {

    @Id
    @Builder.Default
    private String id = "default";

    @Column(name = "company_name", nullable = false)
    @Builder.Default
    private String companyName = "Kumbu Lda";

    @Column(name = "reference_prefix", nullable = false)
    @Builder.Default
    private String referencePrefix = "KMB";

    @Column(name = "gate_min_dau", nullable = false)
    @Builder.Default
    private int gateMinDau = 200;

    @Column(name = "gate_min_listings", nullable = false)
    @Builder.Default
    private int gateMinListings = 300;

    @Column(name = "gate_min_chats", nullable = false)
    @Builder.Default
    private int gateMinChats = 30;

    @Column(name = "default_max_listings", nullable = false)
    @Builder.Default
    private int defaultMaxListings = 3;

    @Column(name = "payment_expiry_hours", nullable = false)
    @Builder.Default
    private int paymentExpiryHours = 48;

    @Column(name = "charging_enabled", nullable = false)
    @Builder.Default
    private boolean chargingEnabled = false;

    @Column(name = "default_payment_provider_id", nullable = false)
    @Builder.Default
    private String defaultPaymentProviderId = "prov_multicaixa_express";

    @Column(name = "payment_sla_hours", nullable = false)
    @Builder.Default
    private int paymentSlaHours = 24;

    @Column(name = "bank_transfers_enabled", nullable = false)
    @Builder.Default
    private boolean bankTransfersEnabled = false;

    @Column(name = "leads_min_servicos_chats", nullable = false)
    @Builder.Default
    private int leadsMinServicosChats = 100;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
