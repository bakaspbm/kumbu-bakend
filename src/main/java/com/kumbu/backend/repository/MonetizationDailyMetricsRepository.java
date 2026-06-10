package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.MonetizationDailyMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface MonetizationDailyMetricsRepository extends JpaRepository<MonetizationDailyMetrics, LocalDate> {

    Optional<MonetizationDailyMetrics> findFirstByOrderByMetricDateDesc();
}
