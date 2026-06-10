package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.*;
import com.kumbu.backend.domain.enums.MonetizationFeatureType;
import com.kumbu.backend.domain.enums.PlatformPaymentStatus;
import com.kumbu.backend.domain.enums.PromotionType;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.*;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MonetizationPaymentService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final MonetizationPaymentRepository paymentRepository;
    private final MonetizationProductRepository productRepository;
    private final MonetizationPaymentProviderRepository providerRepository;
    private final MonetizationPhaseService phaseService;
    private final MonetizationFulfillmentService fulfillmentService;
    private final CatalogProductRepository catalogProductRepository;
    private final SecurityUtils securityUtils;
    private final MonetizationAdminConfigService configService;
    private final MonetizationMetricsService metricsService;
    private final MonetizationVipTrackingService vipTrackingService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPaymentProviders() {
        var settings = configService.getSettings();
        return providerRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .filter(p -> isProviderVisible(p, settings))
                .map(this::toProviderMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRecommendedProvider() {
        var settings = configService.getSettings();
        return providerRepository.findById(settings.getDefaultPaymentProviderId())
                .filter(MonetizationPaymentProvider::isActive)
                .map(this::toProviderMap)
                .orElseGet(() -> providerRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                        .findFirst()
                        .map(this::toProviderMap)
                        .orElse(Map.of()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listPaymentProvidersPublic() {
        return Map.of(
                "providers", listPaymentProviders(),
                "recommended", getRecommendedProvider(),
                "payment_policy", Map.of(
                        "primary", "MULTICAIXA_EXPRESS",
                        "message", "Recomendamos Multicaixa Express — confirmação mais rápida"
                ),
                "payment_context", platformPaymentContext()
        );
    }

    private boolean isProviderVisible(MonetizationPaymentProvider provider, MonetizationSettings settings) {
        if (provider.getProviderType().name().equals("BANK_TRANSFER")) {
            return settings.isBankTransfersEnabled();
        }
        return true;
    }

    @Transactional
    public Map<String, Object> initiatePayment(String productId, String providerId, String targetType, String targetId) {
        UUID userId = securityUtils.currentUserId();

        assertChargingAllowed();

        MonetizationProduct product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Produto não encontrado"));

        if (!product.isActive()) {
            throw ApiException.badRequest("Produto indisponível");
        }

        if (!phaseService.isFeatureActive(product.getFeatureType())) {
            throw ApiException.badRequest("Esta funcionalidade ainda não está activa na plataforma");
        }

        assertProductPurchasable(product);

        var settings = configService.getSettings();
        if (providerId == null || providerId.isBlank()) {
            providerId = settings.getDefaultPaymentProviderId();
        }

        MonetizationPaymentProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> ApiException.notFound("Método de pagamento não encontrado"));

        if (!provider.isActive() || !isProviderVisible(provider, settings)) {
            throw ApiException.badRequest("Método de pagamento indisponível. Use Multicaixa Express.");
        }

        validateTarget(product.getFeatureType(), targetType, targetId, userId);

        String reference = generateReference(settings.getReferencePrefix());
        Instant expiresAt = Instant.now().plus(settings.getPaymentExpiryHours(), ChronoUnit.HOURS);

        MonetizationPayment payment = MonetizationPayment.builder()
                .userId(userId)
                .productId(productId)
                .providerId(providerId)
                .amountKz(product.getPriceKz())
                .referenceCode(reference)
                .status(PlatformPaymentStatus.PENDING)
                .paymentMethod(provider.getProviderType().name())
                .targetType(targetType)
                .targetId(targetId)
                .expiresAt(expiresAt)
                .build();

        paymentRepository.save(payment);

        Map<String, Object> result = toPaymentMap(payment);
        result.put("product", Map.of(
                "id", product.getId(),
                "name", product.getName(),
                "price_kz", product.getPriceKz(),
                "price_label", MonetizationPhaseService.formatKz(product.getPriceKz())
        ));
        result.put("provider", toProviderMap(provider));
        result.put("payment_instructions", buildInstructions(provider, reference, product.getPriceKz()));
        result.put("payment_context", platformPaymentContext());
        return result;
    }

    @Transactional
    public Map<String, Object> submitPaymentProof(UUID paymentId, String proofUrl, String proofNote) {
        MonetizationPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Pagamento não encontrado"));

        if (!payment.getUserId().equals(securityUtils.currentUserId())) {
            throw ApiException.forbidden("Não autorizado");
        }

        if (payment.getStatus() != PlatformPaymentStatus.PENDING
                && payment.getStatus() != PlatformPaymentStatus.AWAITING_CONFIRMATION) {
            throw ApiException.conflict("Pagamento não pode receber comprovativo neste estado");
        }

        if (payment.getExpiresAt().isBefore(Instant.now())) {
            payment.setStatus(PlatformPaymentStatus.EXPIRED);
            paymentRepository.save(payment);
            throw ApiException.badRequest("Pagamento expirado. Crie um novo pedido.");
        }

        payment.setProofUrl(proofUrl);
        payment.setProofNote(proofNote);
        payment.setStatus(PlatformPaymentStatus.AWAITING_CONFIRMATION);
        paymentRepository.save(payment);

        return toPaymentMap(payment);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMyPayments(int page, int size) {
        UUID userId = securityUtils.currentUserId();
        Page<MonetizationPayment> payments = paymentRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", payments.getContent().stream().map(this::toPaymentMap).toList());
        result.put("page", page);
        result.put("size", size);
        result.put("total", payments.getTotalElements());
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPayment(UUID paymentId) {
        MonetizationPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Pagamento não encontrado"));

        if (!payment.getUserId().equals(securityUtils.currentUserId()) && !securityUtils.isAdmin()) {
            throw ApiException.forbidden("Não autorizado");
        }

        return toPaymentMap(payment);
    }

    @Transactional
    public Map<String, Object> confirmPayment(UUID paymentId, String note) {
        MonetizationPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Pagamento não encontrado"));

        if (payment.getStatus() != PlatformPaymentStatus.AWAITING_CONFIRMATION
                && payment.getStatus() != PlatformPaymentStatus.PENDING) {
            throw ApiException.conflict("Pagamento não pode ser confirmado neste estado: " + payment.getStatus());
        }

        payment.setStatus(PlatformPaymentStatus.CONFIRMED);
        payment.setConfirmedAt(Instant.now());
        payment.setConfirmedBy(securityUtils.currentUserId());
        paymentRepository.save(payment);

        fulfillmentService.fulfill(payment);

        Map<String, Object> result = toPaymentMap(payment);
        result.put("message", "Pagamento confirmado. Benefícios activados.");
        result.put("admin_note", note);
        return result;
    }

    @Transactional
    public Map<String, Object> rejectPayment(UUID paymentId, String reason) {
        MonetizationPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound("Pagamento não encontrado"));

        payment.setStatus(PlatformPaymentStatus.REJECTED);
        payment.setRejectedReason(reason);
        paymentRepository.save(payment);

        return toPaymentMap(payment);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listPendingPayments(int page, int size) {
        Page<MonetizationPayment> payments = paymentRepository.findByStatusOrderByCreatedAtDesc(
                PlatformPaymentStatus.AWAITING_CONFIRMATION, PageRequest.of(page, size));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", payments.getContent().stream().map(this::toPaymentMap).toList());
        result.put("page", page);
        result.put("size", size);
        result.put("total", payments.getTotalElements());
        result.put("pending_count", paymentRepository.countByStatus(PlatformPaymentStatus.AWAITING_CONFIRMATION));
        return result;
    }

    private void validateTarget(MonetizationFeatureType featureType, String targetType, String targetId, UUID userId) {
        boolean needsListing = switch (featureType) {
            case HIGHLIGHT_TOP, HIGHLIGHT_SIMPLE, HIGHLIGHT_URGENT, BOOST, PREMIUM_HIGHLIGHT -> true;
            default -> false;
        };

        if (needsListing) {
            if (targetId == null || targetId.isBlank()) {
                throw ApiException.badRequest("Indique o anúncio (target_id) para este produto");
            }
            CatalogProduct listing = catalogProductRepository.findById(targetId)
                    .orElseThrow(() -> ApiException.notFound("Anúncio não encontrado"));
            if (listing.getSellerId() == null || !listing.getSellerId().equals(userId)) {
                throw ApiException.forbidden("Só pode promover os seus próprios anúncios");
            }
        }
    }

    private String generateReference(String prefix) {
        String p = (prefix != null && !prefix.isBlank()) ? prefix.toUpperCase() : "KMB";
        return p + (100000 + RANDOM.nextInt(900000));
    }

    public static Map<String, Object> platformPaymentContext() {
        return Map.of(
                "purpose", "PLATFORM_FEATURE",
                "description", "Pagamento à plataforma Kumbu por serviços premium (destaque, VIP, verificação, boost, etc.)",
                "not_for", "MARKETPLACE_CHECKOUT",
                "note", "Compras de produtos entre utilizadores usam /api/v1/store/payment-methods — pagamento directo ao vendedor, não à Kumbu"
        );
    }

    void assertChargingAllowed() {
        if (securityUtils.isAdmin()) {
            return;
        }
        var settings = configService.getSettings();
        if (!settings.isChargingEnabled()) {
            Map<String, Object> gate = metricsService.checkMonetizationGate();
            throw ApiException.forbidden(
                    "Monetização ainda não activa. " + gate.get("message")
                            + " A plataforma ainda está em fase de crescimento — anunciar é grátis por agora.");
        }
    }

    void assertProductPurchasable(MonetizationProduct product) {
        if (product.getFeatureType() == MonetizationFeatureType.PAID_LEADS) {
            if (!vipTrackingService.isLeadsAvailableForServicos()) {
                throw ApiException.badRequest(
                        "Leads pagos ainda não disponíveis. Priorize VIP em Serviços até atingir volume de procura.");
            }
        }
    }

    private List<String> buildInstructions(MonetizationPaymentProvider provider, String reference, long amountKz) {
        List<String> steps = new ArrayList<>();
        steps.add("Valor: " + MonetizationPhaseService.formatKz(amountKz));
        steps.add("Referência: " + reference);
        steps.add("—");

        switch (provider.getProviderType()) {
            case MULTICAIXA_EXPRESS -> {
                steps.add("1. Abra Multicaixa Express");
                steps.add("2. Transferência → Para número: " + nullSafe(provider.getPhoneNumber()));
                steps.add("3. Valor: " + MonetizationPhaseService.formatKz(amountKz));
                steps.add("4. Motivo/Descrição: " + reference);
            }
            case BANK_TRANSFER -> {
                steps.add("Banco: " + nullSafe(provider.getBankName()));
                steps.add("Titular: " + nullSafe(provider.getAccountHolder()));
                steps.add("Conta: " + nullSafe(provider.getAccountNumber()));
                if (provider.getIban() != null) steps.add("IBAN: " + provider.getIban());
                steps.add("Motivo da transferência: " + reference);
            }
            case EMIS_REFERENCE -> {
                steps.add("Entidade: KUMBU (configurar junto à EMIS)");
                steps.add("Referência: " + reference);
                steps.add("Valor: " + MonetizationPhaseService.formatKz(amountKz));
            }
        }

        if (provider.getInstructions() != null) {
            steps.add("—");
            steps.addAll(Arrays.asList(provider.getInstructions().split("\n")));
        }

        steps.add("—");
        steps.add("Após pagar, envie o comprovativo na app (até 48h).");
        return steps;
    }

    private String nullSafe(String value) {
        return value != null ? value : "—";
    }

    private Map<String, Object> toProviderMap(MonetizationPaymentProvider provider) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", provider.getId());
        map.put("name", provider.getName());
        map.put("provider_type", provider.getProviderType().name());
        map.put("bank_name", provider.getBankName());
        map.put("account_holder", provider.getAccountHolder());
        map.put("account_number", provider.getAccountNumber());
        map.put("iban", provider.getIban());
        map.put("phone_number", provider.getPhoneNumber());
        map.put("is_default", provider.isDefault());
        map.put("payment_purpose", "PLATFORM_FEATURE");
        return map;
    }

    Map<String, Object> toPaymentMap(MonetizationPayment payment) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", payment.getId());
        map.put("user_id", payment.getUserId());
        map.put("product_id", payment.getProductId());
        map.put("provider_id", payment.getProviderId());
        map.put("amount_kz", payment.getAmountKz());
        map.put("amount_label", MonetizationPhaseService.formatKz(payment.getAmountKz()));
        map.put("reference_code", payment.getReferenceCode());
        map.put("status", payment.getStatus().name());
        map.put("payment_method", payment.getPaymentMethod());
        map.put("target_type", payment.getTargetType());
        map.put("target_id", payment.getTargetId());
        map.put("proof_url", payment.getProofUrl());
        map.put("proof_note", payment.getProofNote());
        map.put("confirmed_at", payment.getConfirmedAt());
        map.put("rejected_reason", payment.getRejectedReason());
        map.put("expires_at", payment.getExpiresAt());
        map.put("created_at", payment.getCreatedAt());
        return map;
    }
}
