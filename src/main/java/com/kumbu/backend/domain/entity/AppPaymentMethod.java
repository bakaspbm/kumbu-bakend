package com.kumbu.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "app_payment_methods")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPaymentMethod {

    @Id
    private String id;

    @Column(nullable = false)
    private String label;

    @Column(name = "icon_key", nullable = false)
    @Builder.Default
    private String iconKey = "payment";

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;
}
