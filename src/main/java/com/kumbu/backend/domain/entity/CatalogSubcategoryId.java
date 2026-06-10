package com.kumbu.backend.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogSubcategoryId implements Serializable {
    private String categoryId;
    private String id;
}
