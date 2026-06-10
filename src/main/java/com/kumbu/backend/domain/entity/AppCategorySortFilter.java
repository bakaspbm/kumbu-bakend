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
@Table(name = "app_category_sort_filters")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppCategorySortFilter {

    @Id
    private String id;

    @Column(nullable = false)
    private String label;

    @Column(name = "sort_mode", nullable = false)
    @Builder.Default
    private String sortMode = "default";

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
