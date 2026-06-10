package com.kumbu.backend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "catalog_categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogCategory {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(name = "icon_key", nullable = false)
    @Builder.Default
    private String iconKey = "category";

    @Column(name = "accent_hex", nullable = false)
    @Builder.Default
    private String accentHex = "C62828";

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private String kind = "product";
}
