package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.AppCategorySortFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppCategorySortFilterRepository extends JpaRepository<AppCategorySortFilter, String> {
    List<AppCategorySortFilter> findAllByOrderBySortOrderAsc();
}
