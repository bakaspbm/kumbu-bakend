package com.kumbu.backend.service;

import com.kumbu.backend.config.CacheNames;
import com.kumbu.backend.domain.entity.*;
import com.kumbu.backend.domain.enums.ApprovalRequestStatus;
import com.kumbu.backend.domain.enums.MonetizationFeatureType;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.*;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonetizationPhaseService {

    private final MonetizationPhaseRepository phaseRepository;
    private final MonetizationFeatureRepository featureRepository;
    private final MonetizationProductRepository productRepository;
    private final MonetizationApprovalRequestRepository approvalRepository;
    private final MonetizationMetricsService metricsService;
    private final AdminAuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.MONETIZATION, key = "'catalog'")
    public Map<String, Object> getPublicCatalog() {
        List<Map<String, Object>> phases = phaseRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toPhaseMap)
                .collect(Collectors.toList());

        Map<String, Object> gate = metricsService.checkMonetizationGate();

        return Map.of(
                "phases", phases,
                "gate", gate,
                "active_features", listActiveFeatureTypes(),
                "available_products", listAvailableProducts(),
                "payment_context", MonetizationPaymentService.platformPaymentContext()
        );
    }

    @Transactional(readOnly = true)
    public boolean isFeatureActive(MonetizationFeatureType type) {
        return featureRepository.existsByFeatureTypeAndActiveTrue(type);
    }

    @Transactional(readOnly = true)
    public List<String> listActiveFeatureTypes() {
        return featureRepository.findByActiveTrue().stream()
                .map(f -> f.getFeatureType().name())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAvailableProducts() {
        Set<MonetizationFeatureType> activeTypes = featureRepository.findByActiveTrue().stream()
                .map(MonetizationFeature::getFeatureType)
                .collect(Collectors.toSet());

        return productRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .filter(p -> activeTypes.contains(p.getFeatureType()))
                .map(this::toProductMap)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminOverview() {
        Map<String, Object> gate = metricsService.checkMonetizationGate();
        metricsService.computeAndStoreToday();

        List<Map<String, Object>> phases = phaseRepository.findAllByOrderBySortOrderAsc().stream()
                .map(phase -> {
                    Map<String, Object> map = toPhaseMap(phase);
                    List<MonetizationFeature> features = featureRepository.findByPhaseIdOrderBySortOrderAsc(phase.getId());
                    map.put("features", features.stream().map(this::toFeatureMap).collect(Collectors.toList()));
                    map.put("active_features_count", features.stream().filter(MonetizationFeature::isActive).count());
                    map.put("total_features_count", features.size());
                    return map;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> pendingApprovals = approvalRepository
                .findByStatusOrderByRequestedAtDesc(ApprovalRequestStatus.PENDING).stream()
                .map(this::toApprovalMap)
                .collect(Collectors.toList());

        return Map.of(
                "phases", phases,
                "gate", gate,
                "pending_approvals", pendingApprovals,
                "pending_approvals_count", pendingApprovals.size(),
                "payment_context", MonetizationPaymentService.platformPaymentContext()
        );
    }

    @Transactional
    public Map<String, Object> requestFeatureActivation(String featureId) {
        MonetizationFeature feature = featureRepository.findById(featureId)
                .orElseThrow(() -> ApiException.notFound("Feature não encontrada"));

        if (feature.isActive()) {
            throw ApiException.conflict("Feature já está activa");
        }

        if (approvalRepository.existsByFeatureIdAndStatus(featureId, ApprovalRequestStatus.PENDING)) {
            throw ApiException.conflict("Já existe um pedido pendente para esta feature");
        }

        Map<String, Object> gate = metricsService.checkMonetizationGate();
        if (!(boolean) gate.get("gate_ready")) {
            throw ApiException.badRequest("Gatilhos de monetização ainda não atingidos: " + gate.get("message"));
        }

        MonetizationApprovalRequest request = MonetizationApprovalRequest.builder()
                .requestType("FEATURE_ACTIVATION")
                .phaseId(feature.getPhaseId())
                .featureId(featureId)
                .status(ApprovalRequestStatus.PENDING)
                .metricsSnapshot(gate)
                .message("Pedido de activação: " + feature.getName())
                .build();

        approvalRepository.save(request);
        return toApprovalMap(request);
    }

    @Transactional
    @CacheEvict(value = CacheNames.MONETIZATION, allEntries = true)
    public Map<String, Object> approveRequest(UUID requestId, String note) {
        MonetizationApprovalRequest request = approvalRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Pedido não encontrado"));

        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw ApiException.conflict("Pedido já foi processado");
        }

        UUID adminId = securityUtils.currentUserId();
        request.setStatus(ApprovalRequestStatus.APPROVED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(adminId);
        request.setReviewNote(note);
        approvalRepository.save(request);

        if (request.getFeatureId() != null) {
            MonetizationFeature feature = featureRepository.findById(request.getFeatureId())
                    .orElseThrow(() -> ApiException.notFound("Feature não encontrada"));
            feature.setActive(true);
            feature.setActivatedAt(Instant.now());
            feature.setActivatedBy(adminId);
            featureRepository.save(feature);
        }

        auditLogRepository.save(AdminAuditLog.builder()
                .actorId(adminId)
                .action("monetization.feature_approved")
                .entity("monetization_feature")
                .entityId(request.getFeatureId())
                .payload(Map.of("request_id", requestId.toString(), "note", note != null ? note : ""))
                .build());

        return toApprovalMap(request);
    }

    @Transactional
    public Map<String, Object> rejectRequest(UUID requestId, String note) {
        MonetizationApprovalRequest request = approvalRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Pedido não encontrado"));

        if (request.getStatus() != ApprovalRequestStatus.PENDING) {
            throw ApiException.conflict("Pedido já foi processado");
        }

        request.setStatus(ApprovalRequestStatus.REJECTED);
        request.setReviewedAt(Instant.now());
        request.setReviewedBy(securityUtils.currentUserId());
        request.setReviewNote(note);
        approvalRepository.save(request);

        return toApprovalMap(request);
    }

    @Transactional
    @CacheEvict(value = CacheNames.MONETIZATION, allEntries = true)
    public Map<String, Object> activateFeatureDirectly(String featureId) {
        MonetizationFeature feature = featureRepository.findById(featureId)
                .orElseThrow(() -> ApiException.notFound("Feature não encontrada"));

        feature.setActive(true);
        feature.setActivatedAt(Instant.now());
        feature.setActivatedBy(securityUtils.currentUserId());
        featureRepository.save(feature);

        return toFeatureMap(feature);
    }

    @Transactional
    @CacheEvict(value = CacheNames.MONETIZATION, allEntries = true)
    public Map<String, Object> deactivateFeature(String featureId) {
        MonetizationFeature feature = featureRepository.findById(featureId)
                .orElseThrow(() -> ApiException.notFound("Feature não encontrada"));

        feature.setActive(false);
        featureRepository.save(feature);
        return toFeatureMap(feature);
    }

    private Map<String, Object> toPhaseMap(MonetizationPhase phase) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", phase.getId());
        map.put("name", phase.getName());
        map.put("description", phase.getDescription());
        map.put("min_users", phase.getMinUsers());
        map.put("max_users", phase.getMaxUsers());
        map.put("sort_order", phase.getSortOrder());
        return map;
    }

    private Map<String, Object> toFeatureMap(MonetizationFeature feature) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", feature.getId());
        map.put("phase_id", feature.getPhaseId());
        map.put("feature_type", feature.getFeatureType().name());
        map.put("name", feature.getName());
        map.put("description", feature.getDescription());
        map.put("is_active", feature.isActive());
        map.put("activated_at", feature.getActivatedAt());
        map.put("requires_approval", feature.isRequiresApproval());
        map.put("sort_order", feature.getSortOrder());
        return map;
    }

    private Map<String, Object> toProductMap(MonetizationProduct product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.getId());
        map.put("feature_type", product.getFeatureType().name());
        map.put("name", product.getName());
        map.put("description", product.getDescription());
        map.put("price_kz", product.getPriceKz());
        map.put("price_label", formatKz(product.getPriceKz()));
        map.put("duration_days", product.getDurationDays());
        map.put("max_listings", product.getMaxListings());
        map.put("category_hint", product.getCategoryHint());
        map.put("metadata", product.getMetadata());
        return map;
    }

    private Map<String, Object> toApprovalMap(MonetizationApprovalRequest request) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", request.getId());
        map.put("request_type", request.getRequestType());
        map.put("phase_id", request.getPhaseId());
        map.put("feature_id", request.getFeatureId());
        map.put("status", request.getStatus().name());
        map.put("metrics_snapshot", request.getMetricsSnapshot());
        map.put("message", request.getMessage());
        map.put("requested_at", request.getRequestedAt());
        map.put("reviewed_at", request.getReviewedAt());
        map.put("review_note", request.getReviewNote());
        return map;
    }

    static String formatKz(long amount) {
        return String.format("%,d Kz", amount).replace(',', '.');
    }
}
