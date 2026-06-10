package com.kumbu.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_marketing_blocks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppMarketingBlock {

    @Id
    private String id;

    @Column(nullable = false)
    private String kind;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    @Builder.Default
    private String subtitle = "";

    @Column(name = "gradient_from", nullable = false)
    private String gradientFrom;

    @Column(name = "gradient_to", nullable = false)
    private String gradientTo;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
