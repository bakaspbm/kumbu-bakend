package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.MonetizationProduct;
import com.kumbu.backend.domain.enums.MonetizationFeatureType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonetizationProductRepository extends JpaRepository<MonetizationProduct, String> {

    List<MonetizationProduct> findByActiveTrueOrderBySortOrderAsc();

    List<MonetizationProduct> findByFeatureTypeAndActiveTrueOrderBySortOrderAsc(MonetizationFeatureType featureType);

    List<MonetizationProduct> findByCategoryIdAndActiveTrueOrderBySortOrderAsc(String categoryId);

    List<MonetizationProduct> findByCategoryIdOrderBySortOrderAsc(String categoryId);
}
