package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.AppSupportSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSupportSettingsRepository extends JpaRepository<AppSupportSettings, String> {
}
