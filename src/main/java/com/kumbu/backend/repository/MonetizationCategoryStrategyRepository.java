package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.MonetizationCategoryStrategy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonetizationCategoryStrategyRepository extends JpaRepository<MonetizationCategoryStrategy, String> {

    List<MonetizationCategoryStrategy> findByActiveTrueOrderBySortOrderAsc();

    List<MonetizationCategoryStrategy> findAllByOrderBySortOrderAsc();
}
