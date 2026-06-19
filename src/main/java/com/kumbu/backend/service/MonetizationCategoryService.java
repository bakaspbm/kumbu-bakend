package com.kumbu.backend.service;

import com.kumbu.backend.config.CacheNames;
import com.kumbu.backend.domain.entity.MonetizationCategoryStrategy;
import com.kumbu.backend.domain.entity.MonetizationFeature;
import com.kumbu.backend.domain.entity.MonetizationProduct;
import com.kumbu.backend.domain.enums.MonetizationFeatureType;
import com.kumbu.backend.dto.monetization.AdminUpdateCategoryStrategyRequest;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.CatalogCategoryRepository;
import com.kumbu.backend.repository.MonetizationCategoryStrategyRepository;
import com.kumbu.backend.repository.MonetizationFeatureRepository;
import com.kumbu.backend.repository.MonetizationProductRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonetizationCategoryService {

    private static final Map<String, String> CATEGORY_ALIASES = Map.of(
            "telemoveis", "eletronicos"
    );

    private final MonetizationCategoryStrategyRepository strategyRepository;
    private final MonetizationProductRepository productRepository;
    private final MonetizationFeatureRepository featureRepository;
    private final CatalogCategoryRepository categoryRepository;
    private final SecurityUtils securityUtils;
    private final MonetizationVipTrackingService vipTrackingService;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.MONETIZATION, key = "'category:' + #categoryId")
    public Map<String, Object> getCategoryMonetization(String categoryId) {
        categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Categoria não encontrada"));

        MonetizationCategoryStrategy strategy = strategyRepository.findById(categoryId)
                .or(() -> Optional.ofNullable(CATEGORY_ALIASES.get(categoryId))
                        .flatMap(strategyRepository::findById))
                .orElseThrow(() -> ApiException.notFound("Estratégia de monetização não configurada para esta categoria"));

        String productCategoryId = strategyRepository.existsById(categoryId) ? categoryId : strategy.getCategoryId();

        Set<MonetizationFeatureType> activeFeatures = featureRepository.findByActiveTrue().stream()
                .map(MonetizationFeature::getFeatureType)
                .collect(Collectors.toSet());

        List<Map<String, Object>> products = productRepository
                .findByCategoryIdAndActiveTrueOrderBySortOrderAsc(productCategoryId).stream()
                .filter(p -> activeFeatures.contains(p.getFeatureType()))
                .filter(p -> strategy.getEnabledFeatureTypes().isEmpty()
                        || strategy.getEnabledFeatureTypes().contains(p.getFeatureType().name()))
                .filter(p -> isProductVisibleForCategory(p, productCategoryId))
                .map(p -> {
                    Map<String, Object> map = toProductMap(p);
                    map.put("is_primary", strategy.getPrimaryMonetization().equals(p.getFeatureType().name()));
                    return map;
                })
                .toList();

        Map<String, Object> primary = products.stream()
                .filter(p -> strategy.getPrimaryMonetization().equals(p.get("feature_type")))
                .findFirst()
                .orElse(products.isEmpty() ? null : products.get(0));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("category_id", productCategoryId);
        result.put("requested_category_id", categoryId);
        result.put("strategy", toStrategyMap(strategy));
        result.put("primary_product", primary);
        result.put("products", products);
        result.put("summary_table", Map.of(
                "primary_monetization", strategy.getPrimaryMonetization(),
                "revenue_tier", strategy.getRevenueTier()
        ));
        if ("servicos".equals(productCategoryId)) {
            result.put("leads_available", vipTrackingService.isLeadsAvailableForServicos());
            result.put("vip_first_message", "Receba mais clientes — VIP primeiro. Leads pagos só com volume.");
        }
        return result;
    }

    private boolean isProductVisibleForCategory(MonetizationProduct product, String categoryId) {
        if (product.getFeatureType() == MonetizationFeatureType.PAID_LEADS
                && MonetizationVipTrackingService.SERVICOS_CATEGORY.equals(categoryId)) {
            return vipTrackingService.isLeadsAvailableForServicos();
        }
        return true;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllStrategies() {
        return strategyRepository.findAllByOrderBySortOrderAsc().stream()
                .map(s -> {
                    Map<String, Object> map = toStrategyMap(s);
                    long productCount = productRepository.findByCategoryIdOrderBySortOrderAsc(s.getCategoryId()).size();
                    map.put("product_count", productCount);
                    return map;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStrategy(String categoryId) {
        MonetizationCategoryStrategy strategy = strategyRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Estratégia não encontrada"));
        Map<String, Object> map = toStrategyMap(strategy);
        map.put("products", productRepository.findByCategoryIdOrderBySortOrderAsc(categoryId).stream()
                .map(this::toProductMap).toList());
        return map;
    }

    @Transactional
    @CacheEvict(value = CacheNames.MONETIZATION, allEntries = true)
    public Map<String, Object> updateStrategy(String categoryId, AdminUpdateCategoryStrategyRequest request) {
        MonetizationCategoryStrategy strategy = strategyRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Estratégia não encontrada"));

        if (request.getPrimaryMonetization() != null) strategy.setPrimaryMonetization(request.getPrimaryMonetization());
        if (request.getSecondaryMonetizations() != null) strategy.setSecondaryMonetizations(request.getSecondaryMonetizations());
        if (request.getStrategyTitle() != null) strategy.setStrategyTitle(request.getStrategyTitle());
        if (request.getStrategyDescription() != null) strategy.setStrategyDescription(request.getStrategyDescription());
        if (request.getWhyDescription() != null) strategy.setWhyDescription(request.getWhyDescription());
        if (request.getCtaMessage() != null) strategy.setCtaMessage(request.getCtaMessage());
        if (request.getCtaButtonLabel() != null) strategy.setCtaButtonLabel(request.getCtaButtonLabel());
        if (request.getRevenueTier() != null) strategy.setRevenueTier(request.getRevenueTier());
        if (request.getEnabledFeatureTypes() != null) strategy.setEnabledFeatureTypes(request.getEnabledFeatureTypes());
        if (request.getActive() != null) strategy.setActive(request.getActive());
        if (request.getSortOrder() != null) strategy.setSortOrder(request.getSortOrder());
        if (request.getMetadata() != null) strategy.setMetadata(request.getMetadata());
        strategy.setUpdatedBy(securityUtils.currentUserId());

        strategyRepository.save(strategy);
        return getStrategy(categoryId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMonetizationMatrix() {
        List<Map<String, Object>> rows = strategyRepository.findAllByOrderBySortOrderAsc().stream()
                .map(s -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("category_id", s.getCategoryId());
                    row.put("strategy_title", s.getStrategyTitle());
                    row.put("primary_monetization", s.getPrimaryMonetization());
                    row.put("secondary_monetizations", s.getSecondaryMonetizations());
                    row.put("revenue_tier", s.getRevenueTier());
                    row.put("cta_message", s.getCtaMessage());
                    row.put("is_active", s.isActive());
                    row.put("products", productRepository.findByCategoryIdOrderBySortOrderAsc(s.getCategoryId()).stream()
                            .map(p -> Map.of(
                                    "id", p.getId(),
                                    "name", p.getName(),
                                    "feature_type", p.getFeatureType().name(),
                                    "price_kz", p.getPriceKz(),
                                    "price_label", MonetizationPhaseService.formatKz(p.getPriceKz()),
                                    "is_active", p.isActive()
                            ))
                            .toList());
                    return row;
                })
                .toList();

        return Map.of(
                "rule", "Cada categoria monetiza diferente — não tratar tudo igual",
                "categories", rows
        );
    }

    String resolveCategoryId(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            throw ApiException.badRequest("category_id é obrigatório");
        }
        if (strategyRepository.existsById(categoryId)) {
            return categoryId;
        }
        String alias = CATEGORY_ALIASES.get(categoryId);
        if (alias != null && strategyRepository.existsById(alias)) {
            return alias;
        }
        return categoryId;
    }

    private Map<String, Object> toStrategyMap(MonetizationCategoryStrategy s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("category_id", s.getCategoryId());
        map.put("primary_monetization", s.getPrimaryMonetization());
        map.put("secondary_monetizations", s.getSecondaryMonetizations());
        map.put("strategy_title", s.getStrategyTitle());
        map.put("strategy_description", s.getStrategyDescription());
        map.put("why_description", s.getWhyDescription());
        map.put("cta_message", s.getCtaMessage());
        map.put("cta_button_label", s.getCtaButtonLabel());
        map.put("revenue_tier", s.getRevenueTier());
        map.put("enabled_feature_types", s.getEnabledFeatureTypes());
        map.put("is_active", s.isActive());
        map.put("sort_order", s.getSortOrder());
        map.put("metadata", s.getMetadata());
        map.put("updated_at", s.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toProductMap(MonetizationProduct p) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", p.getId());
        map.put("category_id", p.getCategoryId());
        map.put("feature_type", p.getFeatureType().name());
        map.put("name", p.getName());
        map.put("description", p.getDescription());
        map.put("price_kz", p.getPriceKz());
        map.put("price_label", MonetizationPhaseService.formatKz(p.getPriceKz()));
        map.put("duration_days", p.getDurationDays());
        map.put("max_listings", p.getMaxListings());
        map.put("is_active", p.isActive());
        map.put("is_primary", false);
        map.put("metadata", p.getMetadata());
        return map;
    }
}
