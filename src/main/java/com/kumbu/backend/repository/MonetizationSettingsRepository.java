package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.MonetizationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonetizationSettingsRepository extends JpaRepository<MonetizationSettings, String> {
}
