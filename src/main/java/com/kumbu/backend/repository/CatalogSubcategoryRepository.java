package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.CatalogSubcategory;
import com.kumbu.backend.domain.entity.CatalogSubcategoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogSubcategoryRepository extends JpaRepository<CatalogSubcategory, CatalogSubcategoryId> {
    List<CatalogSubcategory> findByCategoryIdOrderBySortOrderAsc(String categoryId);
}
