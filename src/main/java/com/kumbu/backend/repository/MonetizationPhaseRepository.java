package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.MonetizationPhase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonetizationPhaseRepository extends JpaRepository<MonetizationPhase, String> {

    List<MonetizationPhase> findAllByOrderBySortOrderAsc();
}
