package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.AppMarketingBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppMarketingBlockRepository extends JpaRepository<AppMarketingBlock, String> {
    List<AppMarketingBlock> findAllByOrderBySortOrderAsc();
}
