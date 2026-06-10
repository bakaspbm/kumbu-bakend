package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.MonetizationFeature;
import com.kumbu.backend.domain.enums.MonetizationFeatureType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonetizationFeatureRepository extends JpaRepository<MonetizationFeature, String> {

    List<MonetizationFeature> findByPhaseIdOrderBySortOrderAsc(String phaseId);

    List<MonetizationFeature> findByActiveTrue();

    Optional<MonetizationFeature> findByFeatureType(MonetizationFeatureType featureType);

    boolean existsByFeatureTypeAndActiveTrue(MonetizationFeatureType featureType);
}
