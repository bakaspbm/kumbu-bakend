package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.MonetizationVipContactStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MonetizationVipContactStatsRepository extends JpaRepository<MonetizationVipContactStats, UUID> {

    Optional<MonetizationVipContactStats> findByUserIdAndCategoryIdAndPeriodMonth(
            UUID userId, String categoryId, LocalDate periodMonth);

    List<MonetizationVipContactStats> findByCategoryIdAndPeriodMonth(String categoryId, LocalDate periodMonth);

    @Query("SELECT COALESCE(SUM(v.contactsCount), 0) FROM MonetizationVipContactStats v WHERE v.categoryId = :categoryId AND v.periodMonth = :periodMonth")
    long sumContactsByCategoryAndMonth(@Param("categoryId") String categoryId, @Param("periodMonth") LocalDate periodMonth);
}
