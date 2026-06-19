package com.kumbu.backend.service;

import com.kumbu.backend.config.CacheNames;
import com.kumbu.backend.domain.entity.CatalogCategory;
import com.kumbu.backend.domain.entity.CatalogProduct;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.enums.ListingKind;
import com.kumbu.backend.dto.catalog.CategoryResponse;
import com.kumbu.backend.dto.catalog.CreateListingRequest;
import com.kumbu.backend.dto.catalog.ListingResponse;
import com.kumbu.backend.dto.catalog.UpdateListingRequest;
import com.kumbu.backend.dto.common.PageResponse;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.dto.catalog.SubcategoryResponse;
import com.kumbu.backend.repository.CatalogCategoryRepository;
import com.kumbu.backend.repository.CatalogSubcategoryRepository;
import com.kumbu.backend.domain.entity.CatalogSubcategory;
import com.kumbu.backend.repository.CatalogProductRepository;
import com.kumbu.backend.repository.UserRepository;
import com.kumbu.backend.security.SecurityUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CatalogService {

    private final CatalogCategoryRepository categoryRepository;
    private final CatalogSubcategoryRepository subcategoryRepository;
    private final CatalogProductRepository productRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final RecommendationService recommendationService;
    private final ProfileCompletenessService profileCompletenessService;
    private final MonetizationAdminConfigService monetizationConfigService;

    public CatalogService(CatalogCategoryRepository categoryRepository,
                          CatalogSubcategoryRepository subcategoryRepository,
                          CatalogProductRepository productRepository,
                          UserRepository userRepository,
                          SecurityUtils securityUtils,
                          @Lazy RecommendationService recommendationService,
                          ProfileCompletenessService profileCompletenessService,
                          MonetizationAdminConfigService monetizationConfigService) {
        this.categoryRepository = categoryRepository;
        this.subcategoryRepository = subcategoryRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.securityUtils = securityUtils;
        this.recommendationService = recommendationService;
        this.profileCompletenessService = profileCompletenessService;
        this.monetizationConfigService = monetizationConfigService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ListingResponse> listFavorites(int page, int size) {
        User user = userRepository.findById(securityUtils.currentUserId()).orElseThrow();
        List<String> favIds = user.getFavorites() != null ? user.getFavorites() : List.of();
        if (favIds.isEmpty()) {
            return PageResponse.<ListingResponse>builder()
                    .content(List.of()).page(page).size(size).totalElements(0).totalPages(0).build();
        }
        List<ListingResponse> items = favIds.stream()
                .map(id -> productRepository.findByIdAndDeletedAtIsNull(id).orElse(null))
                .filter(p -> p != null)
                .map(p -> toListing(p, favIds))
                .toList();
        return PageResponse.<ListingResponse>builder()
                .content(items).page(page).size(size)
                .totalElements(items.size()).totalPages(1).build();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.CATALOG_CATEGORIES, key = "'all'")
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toCategory)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.CATALOG_SUBCATEGORIES, key = "#categoryId")
    public List<SubcategoryResponse> listSubcategories(String categoryId) {
        return subcategoryRepository.findByCategoryIdOrderBySortOrderAsc(categoryId).stream()
                .map(this::toSubcategory)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.CATALOG_SEARCH,
            key = "T(java.util.Objects).hash(#categoryId, #subcategoryId, #sellerId, #featured, #sortMode, #query, #page, #size)")
    public PageResponse<ListingResponse> search(
            String categoryId,
            String subcategoryId,
            UUID sellerId,
            Boolean featured,
            String sortMode,
            String query,
            int page,
            int size) {
        String q = blankToNull(query);
        String queryPattern = q == null ? null : "%" + q.toLowerCase() + "%";
        Page<CatalogProduct> result = productRepository.search(
                blankToNull(categoryId),
                blankToNull(subcategoryId),
                sellerId,
                featured,
                queryPattern,
                PageRequest.of(page, size, resolveSort(sortMode)));

        return toPage(result, page, size);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.CATALOG_FEATURED, key = "#page + ':' + #size")
    public PageResponse<ListingResponse> listFeatured(int page, int size) {
        Page<CatalogProduct> result = productRepository.findPublicFeatured(
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("boostScore"))
                        .and(Sort.by("sortOrder").ascending())));
        return toPage(result, page, size);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.CATALOG_LISTING, key = "#id + ':' + @securityUtils.cacheUserKey()")
    public ListingResponse getListing(String id) {
        CatalogProduct product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> ApiException.notFound("Anúncio não encontrado"));
        assertPubliclyVisible(product, currentUserIdOrNull());
        return toListing(product, currentFavorites());
    }

    private UUID currentUserIdOrNull() {
        try {
            return securityUtils.currentUserId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private void assertPubliclyVisible(CatalogProduct product, UUID viewerId) {
        if (viewerId != null && viewerId.equals(product.getSellerId())) {
            return;
        }
        if (product.isOutOfStock()) {
            throw ApiException.notFound("Anúncio não disponível");
        }
        if (product.getJobListingStatus() != null && !"active".equals(product.getJobListingStatus())) {
            throw ApiException.notFound("Anúncio não disponível");
        }
    }

    @Transactional
    public Map<String, Object> recordListingView(String id) {
        CatalogProduct product = productRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> ApiException.notFound("Anúncio não encontrado"));
        product.setViewCount(product.getViewCount() + 1);
        CatalogProduct saved = productRepository.save(product);

        UUID viewerId = null;
        try {
            viewerId = securityUtils.currentUserId();
        } catch (Exception ignored) {
            // anónimo
        }
        recommendationService.recordView(saved, viewerId);

        return Map.of("view_count", saved.getViewCount());
    }

    @Transactional(readOnly = true)
    public ListingResponse toBrowseListing(CatalogProduct product) {
        List<String> favorites = List.of();
        try {
            favorites = currentFavorites();
        } catch (Exception ignored) {
            // público / sem sessão
        }
        return toListing(product, favorites);
    }

    @Transactional(readOnly = true)
    public ListingResponse toListingResponse(CatalogProduct product) {
        return toListing(product, currentFavorites());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.RECOMMENDATIONS, key = "'listing-rec:' + #productId + ':' + #limit")
    public Map<String, Object> getListingRecommendations(String productId, int limit) {
        return recommendationService.getProductRecommendations(productId, limit);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.CATALOG_SEARCH, allEntries = true),
            @CacheEvict(value = CacheNames.CATALOG_FEATURED, allEntries = true),
            @CacheEvict(value = CacheNames.CATALOG_LISTING, allEntries = true),
            @CacheEvict(value = CacheNames.RECOMMENDATIONS, allEntries = true),
            @CacheEvict(value = CacheNames.JOBS, allEntries = true),
            @CacheEvict(value = CacheNames.ADMIN_STATS, allEntries = true)
    })
    public ListingResponse createListing(CreateListingRequest request) {
        UUID sellerId = securityUtils.currentUserId();
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> ApiException.notFound("Utilizador não encontrado"));
        profileCompletenessService.assertCanPublish(seller);

        if (monetizationConfigService.getSettings().isChargingEnabled()) {
            long currentListings = productRepository.findBySellerIdAndDeletedAtIsNullOrderByCreatedAtDesc(sellerId).size();
            if (currentListings >= seller.getMaxListings()) {
                throw ApiException.badRequest("Limite de anúncios atingido (" + seller.getMaxListings()
                        + "). Faça upgrade para VIP para publicar mais.");
            }
        }

        String id = "prd_" + System.currentTimeMillis();

        CatalogProduct product = CatalogProduct.builder()
                .id(id)
                .categoryId(request.getCategoryId())
                .subcategoryId(request.getSubcategoryId())
                .title(request.getTitle().trim())
                .priceLabel(request.getPriceLabel())
                .description(request.getDescription())
                .deliveryText(request.getDeliveryText())
                .listingKind(parseKind(request.getListingKind()))
                .sellerId(sellerId)
                .imageUrls(request.getImageUrls() != null ? request.getImageUrls() : new ArrayList<>())
                .productMeta(copyMeta(request.getProductMeta()))
                .propertyMeta(copyMeta(request.getPropertyMeta()))
                .jobMeta(copyMeta(request.getJobMeta()))
                .build();

        if (!product.getImageUrls().isEmpty()) {
            product.setImageUrl(product.getImageUrls().get(0));
        }

        return toListing(productRepository.save(product), currentFavorites());
    }

    @Transactional(readOnly = true)
    public List<ListingResponse> listMyListings() {
        UUID sellerId = securityUtils.currentUserId();
        return productRepository.findBySellerIdAndDeletedAtIsNullOrderByCreatedAtDesc(sellerId).stream()
                .map(p -> toListing(p, currentFavorites()))
                .toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.CATALOG_SEARCH, allEntries = true),
            @CacheEvict(value = CacheNames.CATALOG_FEATURED, allEntries = true),
            @CacheEvict(value = CacheNames.CATALOG_LISTING, allEntries = true),
            @CacheEvict(value = CacheNames.RECOMMENDATIONS, allEntries = true),
            @CacheEvict(value = CacheNames.JOBS, allEntries = true),
            @CacheEvict(value = CacheNames.ADMIN_STATS, allEntries = true)
    })
    public void deleteMyListing(String productId) {
        UUID sellerId = securityUtils.currentUserId();
        CatalogProduct product = productRepository.findById(productId)
                .filter(p -> sellerId.equals(p.getSellerId()))
                .orElseThrow(() -> ApiException.notFound("Anúncio não encontrado"));
        product.setDeletedAt(Instant.now());
        productRepository.save(product);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CacheNames.CATALOG_SEARCH, allEntries = true),
            @CacheEvict(value = CacheNames.CATALOG_FEATURED, allEntries = true),
            @CacheEvict(value = CacheNames.CATALOG_LISTING, allEntries = true),
            @CacheEvict(value = CacheNames.RECOMMENDATIONS, allEntries = true),
            @CacheEvict(value = CacheNames.JOBS, allEntries = true),
            @CacheEvict(value = CacheNames.ADMIN_STATS, allEntries = true)
    })
    public ListingResponse updateMyListing(String productId, UpdateListingRequest request) {
        UUID sellerId = securityUtils.currentUserId();
        CatalogProduct product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .filter(p -> sellerId.equals(p.getSellerId()))
                .orElseThrow(() -> ApiException.notFound("Anúncio não encontrado"));

        if (request.getTitle() != null) {
            product.setTitle(request.getTitle().trim());
        }
        if (request.getPriceLabel() != null) {
            product.setPriceLabel(request.getPriceLabel());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getDeliveryText() != null) {
            product.setDeliveryText(request.getDeliveryText());
        }
        if (request.getOutOfStock() != null) {
            product.setOutOfStock(request.getOutOfStock());
        }
        if (request.getImageUrls() != null) {
            List<String> urls = request.getImageUrls().stream()
                    .filter(u -> u != null && !u.isBlank())
                    .map(String::trim)
                    .toList();
            product.setImageUrls(new ArrayList<>(urls));
            product.setImageUrl(urls.isEmpty() ? null : urls.get(0));
        }
        if (request.getProductMeta() != null) {
            product.setProductMeta(copyMeta(request.getProductMeta()));
        }
        if (request.getPropertyMeta() != null) {
            product.setPropertyMeta(copyMeta(request.getPropertyMeta()));
        }
        if (request.getJobMeta() != null) {
            product.setJobMeta(copyMeta(request.getJobMeta()));
        }

        product.setUpdatedAt(Instant.now());
        return toListing(productRepository.save(product), currentFavorites());
    }

    private PageResponse<ListingResponse> toPage(Page<CatalogProduct> page, int pageNum, int size) {
        List<String> favorites = currentFavorites();
        return PageResponse.<ListingResponse>builder()
                .content(page.getContent().stream().map(p -> toListing(p, favorites)).toList())
                .page(pageNum)
                .size(size)
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    private CategoryResponse toCategory(CatalogCategory c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .slug(c.getId())
                .iconKey(c.getIconKey())
                .accentHex(c.getAccentHex())
                .kind(c.getKind())
                .build();
    }

    private SubcategoryResponse toSubcategory(CatalogSubcategory subcategory) {
        return SubcategoryResponse.builder()
                .categoryId(subcategory.getCategoryId())
                .id(subcategory.getId())
                .label(subcategory.getLabel())
                .sortOrder(subcategory.getSortOrder())
                .build();
    }

    private ListingResponse toListing(CatalogProduct p, List<String> favorites) {
        User seller = p.getSellerId() != null
                ? userRepository.findById(p.getSellerId()).orElse(null)
                : null;

        return ListingResponse.builder()
                .id(p.getId())
                .title(p.getTitle())
                .priceLabel(p.getPriceLabel())
                .price(parsePrice(p.getPriceLabel()))
                .city(seller != null ? seller.getCity() : null)
                .region(seller != null ? seller.getRegion() : null)
                .imageUrls(buildImageUrls(p))
                .description(p.getDescription())
                .categoryId(p.getCategoryId())
                .subcategoryId(p.getSubcategoryId())
                .listingKind(p.getListingKind().name().toLowerCase())
                .rating(p.getRating())
                .reviewCount(p.getReviewCount())
                .viewCount(p.getViewCount())
                .featured(p.isFeatured())
                .outOfStock(p.isOutOfStock())
                .jobListingStatus(p.getJobListingStatus())
                .sellerId(p.getSellerId())
                .sellerName(seller != null ? seller.getDisplayName() : null)
                .sellerPhotoUrl(seller != null ? seller.getPhotoUrl() : null)
                .sellerVerified(seller != null && seller.isSellerVerified())
                .createdAt(p.getCreatedAt())
                .propertyMeta(p.getPropertyMeta())
                .jobMeta(p.getJobMeta())
                .productMeta(p.getProductMeta())
                .favorite(favorites.contains(p.getId()))
                .build();
    }

    private List<String> buildImageUrls(CatalogProduct p) {
        if (p.getImageUrls() != null && !p.getImageUrls().isEmpty()) {
            return p.getImageUrls();
        }
        if (p.getImageUrl() != null) {
            return List.of(p.getImageUrl());
        }
        return List.of();
    }

    private List<String> currentFavorites() {
        try {
            UUID id = securityUtils.currentUserId();
            return userRepository.findById(id).map(User::getFavorites).orElse(List.of());
        } catch (Exception e) {
            return List.of();
        }
    }

    private ListingKind parseKind(String kind) {
        if (kind == null) return ListingKind.GENERAL;
        return switch (kind.toLowerCase()) {
            case "property" -> ListingKind.PROPERTY;
            case "job" -> ListingKind.JOB;
            default -> ListingKind.GENERAL;
        };
    }

    static BigDecimal parsePrice(String label) {
        if (label == null) return BigDecimal.ZERO;
        String digits = label.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return BigDecimal.ZERO;
        return new BigDecimal(digits);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static Map<String, Object> copyMeta(Map<String, Object> meta) {
        if (meta == null || meta.isEmpty()) {
            return null;
        }
        return new LinkedHashMap<>(meta);
    }

    private Sort resolveSort(String sortMode) {
        if ("rating_desc".equalsIgnoreCase(sortMode)) {
            return Sort.by(Sort.Direction.DESC, "rating")
                    .and(Sort.by("title").ascending());
        }
        if ("price_asc".equalsIgnoreCase(sortMode)) {
            return Sort.by(Sort.Order.asc("priceLabel"))
                    .and(Sort.by("title").ascending());
        }
        return Sort.by(Sort.Order.desc("boostScore"))
                .and(Sort.by("sortOrder").ascending())
                .and(Sort.by("title").ascending());
    }
}
