package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.CatalogCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CatalogCategoryRepository extends JpaRepository<CatalogCategory, String> {

    List<CatalogCategory> findAllByOrderBySortOrderAsc();
}
