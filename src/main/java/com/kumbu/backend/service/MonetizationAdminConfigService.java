package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.MonetizationPaymentProvider;
import com.kumbu.backend.domain.entity.MonetizationProduct;
import com.kumbu.backend.domain.entity.MonetizationSettings;
import com.kumbu.backend.domain.enums.LocalPaymentProviderType;
import com.kumbu.backend.domain.enums.MonetizationFeatureType;
import com.kumbu.backend.dto.monetization.AdminCreateMonetizationProductRequest;
import com.kumbu.backend.dto.monetization.AdminUpdateMonetizationProductRequest;
import com.kumbu.backend.dto.monetization.AdminUpdateMonetizationSettingsRequest;
import com.kumbu.backend.dto.monetization.AdminUpdatePaymentProviderRequest;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.MonetizationPaymentProviderRepository;
import com.kumbu.backend.repository.MonetizationProductRepository;
import com.kumbu.backend.repository.MonetizationSettingsRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonetizationAdminConfigService {

    private final MonetizationSettingsRepository settingsRepository;
    private final MonetizationProductRepository productRepository;
    private final MonetizationPaymentProviderRepository providerRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public MonetizationSettings getSettings() {
        return settingsRepository.findById("default")
                .orElseGet(this::createDefaultSettings);
    }

    @Transactional
    public Map<String, Object> updateSettings(AdminUpdateMonetizationSettingsRequest request) {
        MonetizationSettings settings = getSettings();

        if (request.getCompanyName() != null) settings.setCompanyName(request.getCompanyName());
        if (request.getReferencePrefix() != null) settings.setReferencePrefix(request.getReferencePrefix());
        if (request.getGateMinDau() != null) settings.setGateMinDau(request.getGateMinDau());
        if (request.getGateMinListings() != null) settings.setGateMinListings(request.getGateMinListings());
        if (request.getGateMinChats() != null) settings.setGateMinChats(request.getGateMinChats());
        if (request.getDefaultMaxListings() != null) settings.setDefaultMaxListings(request.getDefaultMaxListings());
        if (request.getPaymentExpiryHours() != null) settings.setPaymentExpiryHours(request.getPaymentExpiryHours());
        if (request.getChargingEnabled() != null) settings.setChargingEnabled(request.getChargingEnabled());
        if (request.getDefaultPaymentProviderId() != null) settings.setDefaultPaymentProviderId(request.getDefaultPaymentProviderId());
        if (request.getPaymentSlaHours() != null) settings.setPaymentSlaHours(request.getPaymentSlaHours());
        if (request.getBankTransfersEnabled() != null) settings.setBankTransfersEnabled(request.getBankTransfersEnabled());
        if (request.getLeadsMinServicosChats() != null) settings.setLeadsMinServicosChats(request.getLeadsMinServicosChats());
        settings.setUpdatedBy(securityUtils.currentUserId());

        settingsRepository.save(settings);
        syncCompanyNameToProviders(settings.getCompanyName());
        return toSettingsMap(settings);
    }

    @Transactional
    public Map<String, Object> setChargingEnabled(boolean enabled) {
        MonetizationSettings settings = getSettings();
        settings.setChargingEnabled(enabled);
        settings.setUpdatedBy(securityUtils.currentUserId());
        settingsRepository.save(settings);
        Map<String, Object> result = toSettingsMap(settings);
        result.put("message", enabled
                ? "Cobrança activada. Clientes já podem pagar features premium."
                : "Cobrança desactivada. Anunciar volta a ser grátis.");
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllProducts() {
        return productRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(this::toProductMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProduct(String productId) {
        return toProductMap(productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Produto não encontrado")));
    }

    @Transactional
    public Map<String, Object> createProduct(AdminCreateMonetizationProductRequest request) {
        if (request.getId() == null || request.getId().isBlank()) {
            throw ApiException.badRequest("id é obrigatório");
        }
        if (productRepository.existsById(request.getId())) {
            throw ApiException.conflict("Produto já existe");
        }
        if (request.getFeatureType() == null) {
            throw ApiException.badRequest("feature_type é obrigatório");
        }
        if (request.getName() == null || request.getPriceKz() == null) {
            throw ApiException.badRequest("name e price_kz são obrigatórios");
        }

        MonetizationProduct product = MonetizationProduct.builder()
                .id(request.getId())
                .featureType(MonetizationFeatureType.valueOf(request.getFeatureType()))
                .name(request.getName())
                .description(request.getDescription())
                .priceKz(request.getPriceKz())
                .durationDays(request.getDurationDays())
                .maxListings(request.getMaxListings())
                .categoryHint(request.getCategoryHint())
                .categoryId(request.getCategoryId())
                .active(request.getActive() == null || request.getActive())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .metadata(request.getMetadata() != null ? request.getMetadata() : new HashMap<>())
                .createdAt(Instant.now())
                .build();

        return toProductMap(productRepository.save(product));
    }

    @Transactional
    public Map<String, Object> updateProduct(String productId, AdminUpdateMonetizationProductRequest request) {
        MonetizationProduct product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Produto não encontrado"));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPriceKz() != null) product.setPriceKz(request.getPriceKz());
        if (request.getDurationDays() != null) product.setDurationDays(request.getDurationDays());
        if (request.getMaxListings() != null) product.setMaxListings(request.getMaxListings());
        if (request.getCategoryHint() != null) product.setCategoryHint(request.getCategoryHint());
        if (request.getCategoryId() != null) product.setCategoryId(request.getCategoryId());
        if (request.getActive() != null) product.setActive(request.getActive());
        if (request.getSortOrder() != null) product.setSortOrder(request.getSortOrder());
        if (request.getMetadata() != null) product.setMetadata(request.getMetadata());

        return toProductMap(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(String productId) {
        if (!productRepository.existsById(productId)) {
            throw ApiException.notFound("Produto não encontrado");
        }
        productRepository.deleteById(productId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllProviders() {
        return providerRepository.findAll().stream()
                .sorted((a, b) -> Integer.compare(a.getSortOrder(), b.getSortOrder()))
                .map(this::toProviderMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> updateProvider(String providerId, AdminUpdatePaymentProviderRequest request) {
        MonetizationPaymentProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> ApiException.notFound("Provedor não encontrado"));

        if (request.getName() != null) provider.setName(request.getName());
        if (request.getProviderType() != null) {
            provider.setProviderType(LocalPaymentProviderType.valueOf(request.getProviderType()));
        }
        if (request.getBankName() != null) provider.setBankName(request.getBankName());
        if (request.getAccountHolder() != null) provider.setAccountHolder(request.getAccountHolder());
        if (request.getAccountNumber() != null) provider.setAccountNumber(request.getAccountNumber());
        if (request.getIban() != null) provider.setIban(request.getIban());
        if (request.getPhoneNumber() != null) provider.setPhoneNumber(request.getPhoneNumber());
        if (request.getInstructions() != null) provider.setInstructions(request.getInstructions());
        if (request.getActive() != null) provider.setActive(request.getActive());
        if (request.getSortOrder() != null) provider.setSortOrder(request.getSortOrder());

        return toProviderMap(providerRepository.save(provider));
    }

    private MonetizationSettings createDefaultSettings() {
        return settingsRepository.save(MonetizationSettings.builder().id("default").build());
    }

    private void syncCompanyNameToProviders(String companyName) {
        providerRepository.findAll().forEach(p -> {
            p.setAccountHolder(companyName);
            providerRepository.save(p);
        });
    }

    public Map<String, Object> toSettingsMap(MonetizationSettings s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("company_name", s.getCompanyName());
        map.put("reference_prefix", s.getReferencePrefix());
        map.put("gate_min_dau", s.getGateMinDau());
        map.put("gate_min_listings", s.getGateMinListings());
        map.put("gate_min_chats", s.getGateMinChats());
        map.put("default_max_listings", s.getDefaultMaxListings());
        map.put("payment_expiry_hours", s.getPaymentExpiryHours());
        map.put("charging_enabled", s.isChargingEnabled());
        map.put("default_payment_provider_id", s.getDefaultPaymentProviderId());
        map.put("payment_sla_hours", s.getPaymentSlaHours());
        map.put("bank_transfers_enabled", s.isBankTransfersEnabled());
        map.put("leads_min_servicos_chats", s.getLeadsMinServicosChats());
        map.put("updated_at", s.getUpdatedAt());
        map.put("updated_by", s.getUpdatedBy());
        return map;
    }

    private Map<String, Object> toProductMap(MonetizationProduct product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.getId());
        map.put("feature_type", product.getFeatureType().name());
        map.put("name", product.getName());
        map.put("description", product.getDescription());
        map.put("price_kz", product.getPriceKz());
        map.put("price_label", MonetizationPhaseService.formatKz(product.getPriceKz()));
        map.put("duration_days", product.getDurationDays());
        map.put("max_listings", product.getMaxListings());
        map.put("category_hint", product.getCategoryHint());
        map.put("category_id", product.getCategoryId());
        map.put("is_active", product.isActive());
        map.put("sort_order", product.getSortOrder());
        map.put("metadata", product.getMetadata());
        map.put("payment_purpose", "PLATFORM_FEATURE");
        return map;
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
        map.put("instructions", provider.getInstructions());
        map.put("is_active", provider.isActive());
        map.put("is_default", provider.isDefault());
        map.put("sort_order", provider.getSortOrder());
        map.put("payment_purpose", "PLATFORM_FEATURE");
        return map;
    }
}
