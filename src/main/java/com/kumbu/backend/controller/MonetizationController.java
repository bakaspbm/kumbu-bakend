package com.kumbu.backend.controller;

import com.kumbu.backend.dto.monetization.InitiatePaymentRequest;
import com.kumbu.backend.dto.monetization.SubmitPaymentProofRequest;
import com.kumbu.backend.service.MonetizationAdminConfigService;
import com.kumbu.backend.service.MonetizationCategoryService;
import com.kumbu.backend.service.MonetizationMetricsService;
import com.kumbu.backend.service.MonetizationPaymentService;
import com.kumbu.backend.service.MonetizationPhaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/monetization")
@RequiredArgsConstructor
public class MonetizationController {

    private final MonetizationPhaseService phaseService;
    private final MonetizationPaymentService paymentService;
    private final MonetizationMetricsService metricsService;
    private final MonetizationCategoryService categoryService;
    private final MonetizationAdminConfigService configService;

    @GetMapping("/catalog")
    public Map<String, Object> catalog(@RequestParam(required = false) String categoryId) {
        if (categoryId != null && !categoryId.isBlank()) {
            Map<String, Object> category = categoryService.getCategoryMonetization(categoryId);
            Map<String, Object> result = new java.util.LinkedHashMap<>(category);
            result.put("payment_context", MonetizationPaymentService.platformPaymentContext());
            result.put("charging", chargingStatus());
            return result;
        }
        Map<String, Object> result = new java.util.LinkedHashMap<>(phaseService.getPublicCatalog());
        result.put("charging", chargingStatus());
        return result;
    }

    private Map<String, Object> chargingStatus() {
        var settings = configService.getSettings();
        return Map.of(
                "enabled", settings.isChargingEnabled(),
                "message", settings.isChargingEnabled()
                        ? "Features premium disponíveis"
                        : "Anunciar é grátis — monetização ainda não activa"
        );
    }

    @GetMapping("/categories/{categoryId}")
    public Map<String, Object> categoryMonetization(@PathVariable String categoryId) {
        Map<String, Object> result = new java.util.LinkedHashMap<>(categoryService.getCategoryMonetization(categoryId));
        result.put("payment_context", MonetizationPaymentService.platformPaymentContext());
        return result;
    }

    @GetMapping("/gate")
    public Map<String, Object> gateStatus() {
        return metricsService.checkMonetizationGate();
    }

    @GetMapping("/payment-providers")
    public Map<String, Object> paymentProviders() {
        return paymentService.listPaymentProvidersPublic();
    }

    @PostMapping("/payments")
    public Map<String, Object> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request) {
        return paymentService.initiatePayment(
                request.getProductId(),
                request.getProviderId(),
                request.getTargetType(),
                request.getTargetId()
        );
    }

    @GetMapping("/payments")
    public Map<String, Object> myPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return paymentService.getMyPayments(page, size);
    }

    @GetMapping("/payments/{id}")
    public Map<String, Object> getPayment(@PathVariable UUID id) {
        return paymentService.getPayment(id);
    }

    @PostMapping("/payments/{id}/proof")
    public Map<String, Object> submitProof(
            @PathVariable UUID id,
            @RequestBody SubmitPaymentProofRequest request) {
        return paymentService.submitPaymentProof(id, request.getProofUrl(), request.getProofNote());
    }
}
