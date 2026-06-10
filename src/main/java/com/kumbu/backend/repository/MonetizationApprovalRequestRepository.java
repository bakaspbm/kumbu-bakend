package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.MonetizationApprovalRequest;
import com.kumbu.backend.domain.enums.ApprovalRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MonetizationApprovalRequestRepository extends JpaRepository<MonetizationApprovalRequest, UUID> {

    List<MonetizationApprovalRequest> findByStatusOrderByRequestedAtDesc(ApprovalRequestStatus status);

    boolean existsByFeatureIdAndStatus(String featureId, ApprovalRequestStatus status);
}
