package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.AppPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppPaymentMethodRepository extends JpaRepository<AppPaymentMethod, String> {

    List<AppPaymentMethod> findAllByOrderBySortOrderAsc();
}
