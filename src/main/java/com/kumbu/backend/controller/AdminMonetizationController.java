package com.kumbu.backend.controller;

import com.kumbu.backend.dto.monetization.*;
import com.kumbu.backend.service.MonetizationAdminConfigService;
import com.kumbu.backend.service.MonetizationAnalyticsService;
import com.kumbu.backend.service.MonetizationCategoryService;
import com.kumbu.backend.service.MonetizationGateAlertService;
import com.kumbu.backend.service.MonetizationMetricsService;
import com.kumbu.backend.service.MonetizationPaymentService;
import com.kumbu.backend.service.MonetizationPhaseService;
import com.kumbu.backend.service.MonetizationRoutineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/monetization")
@RequiredArgsConstructor
public class AdminMonetizationController {

    private final MonetizationPhaseService phaseService;
    private final MonetizationPaymentService paymentService;
    private final MonetizationMetricsService metricsService;
    private final MonetizationAdminConfigService configService;
    private final MonetizationCategoryService categoryService;
    private final MonetizationRoutineService routineService;
    private final MonetizationAnalyticsService analyticsService;
    private final MonetizationGateAlertService gateAlertService;

    // ── Overview & métricas ──────────────────────────────────────────

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return routineService.getAdminOverviewWithRoutine();
    }

    @PostMapping("/metrics/refresh")
    public Map<String, Object> refreshMetrics() {
        return gateAlertService.evaluateAndNotify();
    }

    @GetMapping("/gate")
    public Map<String, Object> gateStatus() {
        return gateAlertService.getGateStatusForAdmin();
    }

    // ── Configurações gerais (empresa, gatilhos, prazos) ─────────────

    @GetMapping("/settings")
    public Map<String, Object> getSettings() {
        return configService.toSettingsMap(configService.getSettings());
    }

    @PatchMapping("/settings")
    public Map<String, Object> updateSettings(@Valid @RequestBody AdminUpdateMonetizationSettingsRequest request) {
        return configService.updateSettings(request);
    }

    @PostMapping("/settings/enable-charging")
    public Map<String, Object> enableCharging() {
        return routineService.enableCharging();
    }

    @PostMapping("/settings/disable-charging")
    public Map<String, Object> disableCharging() {
        return routineService.disableCharging();
    }

    // ── Analytics & rotina ───────────────────────────────────────────

    @GetMapping("/analytics/key-metrics")
    public Map<String, Object> keyMetrics() {
        return analyticsService.getKeyMetrics();
    }

    @GetMapping("/analytics/price-review")
    public Map<String, Object> priceReview() {
        return Map.of("items", analyticsService.getMonthlyPriceReview());
    }

    @GetMapping("/analytics/overdue-payments")
    public Map<String, Object> overduePayments() {
        List<Map<String, Object>> overdue = analyticsService.getOverduePayments();
        return Map.of(
                "items", overdue,
                "count", overdue.size(),
                "sla_hours", configService.getSettings().getPaymentSlaHours(),
                "urgent", !overdue.isEmpty()
        );
    }

    // ── Fases & features ─────────────────────────────────────────────

    @PostMapping("/features/{featureId}/request-activation")
    public Map<String, Object> requestActivation(@PathVariable String featureId) {
        return phaseService.requestFeatureActivation(featureId);
    }

    @PostMapping("/features/{featureId}/activate")
    public Map<String, Object> activateFeature(@PathVariable String featureId) {
        return phaseService.activateFeatureDirectly(featureId);
    }

    @PostMapping("/features/{featureId}/deactivate")
    public Map<String, Object> deactivateFeature(@PathVariable String featureId) {
        return phaseService.deactivateFeature(featureId);
    }

    @PostMapping("/approvals/{requestId}/approve")
    public Map<String, Object> approveRequest(
            @PathVariable UUID requestId,
            @RequestBody(required = false) ReviewApprovalRequest request) {
        String note = request != null ? request.getNote() : null;
        return phaseService.approveRequest(requestId, note);
    }

    @PostMapping("/approvals/{requestId}/reject")
    public Map<String, Object> rejectRequest(
            @PathVariable UUID requestId,
            @RequestBody(required = false) ReviewApprovalRequest request) {
        String note = request != null ? request.getNote() : null;
        return phaseService.rejectRequest(requestId, note);
    }

    // ── Produtos premium (destaque, VIP, boost, etc.) ───────────────

    @GetMapping("/products")
    public Map<String, Object> listProducts() {
        return Map.of("items", configService.listAllProducts());
    }

    @GetMapping("/products/{productId}")
    public Map<String, Object> getProduct(@PathVariable String productId) {
        return configService.getProduct(productId);
    }

    @PostMapping("/products")
    public Map<String, Object> createProduct(@Valid @RequestBody AdminCreateMonetizationProductRequest request) {
        return configService.createProduct(request);
    }

    @PatchMapping("/products/{productId}")
    public Map<String, Object> updateProduct(
            @PathVariable String productId,
            @RequestBody AdminUpdateMonetizationProductRequest request) {
        return configService.updateProduct(productId, request);
    }

    @DeleteMapping("/products/{productId}")
    public void deleteProduct(@PathVariable String productId) {
        configService.deleteProduct(productId);
    }

    // ── Estratégias por categoria ────────────────────────────────────

    @GetMapping("/categories/matrix")
    public Map<String, Object> monetizationMatrix() {
        return categoryService.getMonetizationMatrix();
    }

    @GetMapping("/categories")
    public Map<String, Object> listCategoryStrategies() {
        return Map.of("items", categoryService.listAllStrategies());
    }

    @GetMapping("/categories/{categoryId}")
    public Map<String, Object> getCategoryStrategy(@PathVariable String categoryId) {
        return categoryService.getStrategy(categoryId);
    }

    @PatchMapping("/categories/{categoryId}")
    public Map<String, Object> updateCategoryStrategy(
            @PathVariable String categoryId,
            @RequestBody AdminUpdateCategoryStrategyRequest request) {
        return categoryService.updateStrategy(categoryId, request);
    }

    @GetMapping("/categories/{categoryId}/products")
    public Map<String, Object> listCategoryProducts(@PathVariable String categoryId) {
        return categoryService.getCategoryMonetization(categoryId);
    }

    // ── Provedores de pagamento (contas bancárias Kumbu) ──────────────

    @GetMapping("/payment-providers")
    public Map<String, Object> paymentProviders() {
        return Map.of("providers", configService.listAllProviders());
    }

    @PatchMapping("/payment-providers/{providerId}")
    public Map<String, Object> updatePaymentProvider(
            @PathVariable String providerId,
            @RequestBody AdminUpdatePaymentProviderRequest request) {
        return configService.updateProvider(providerId, request);
    }

    // ── Pagamentos de features (receita da plataforma) ───────────────

    @GetMapping("/payments/pending")
    public Map<String, Object> pendingPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return paymentService.listPendingPayments(page, size);
    }

    @PostMapping("/payments/{paymentId}/confirm")
    public Map<String, Object> confirmPayment(
            @PathVariable UUID paymentId,
            @RequestBody(required = false) ReviewPaymentRequest request) {
        String note = request != null ? request.getNote() : null;
        return paymentService.confirmPayment(paymentId, note);
    }

    @PostMapping("/payments/{paymentId}/reject")
    public Map<String, Object> rejectPayment(
            @PathVariable UUID paymentId,
            @RequestBody ReviewPaymentRequest request) {
        return paymentService.rejectPayment(paymentId, request.getReason());
    }
}
