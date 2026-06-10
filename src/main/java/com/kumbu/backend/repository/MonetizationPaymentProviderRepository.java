package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.MonetizationPaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonetizationPaymentProviderRepository extends JpaRepository<MonetizationPaymentProvider, String> {

    List<MonetizationPaymentProvider> findByActiveTrueOrderBySortOrderAsc();
}
