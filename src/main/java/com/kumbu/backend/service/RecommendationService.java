package com.kumbu.backend.service;

import com.kumbu.backend.config.CacheNames;
import com.kumbu.backend.domain.entity.CatalogProduct;
import com.kumbu.backend.domain.entity.ProductViewEvent;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.enums.RecommendationReason;
import com.kumbu.backend.dto.catalog.ListingResponse;
import com.kumbu.backend.dto.recommendation.RecommendationItemResponse;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.CatalogProductRepository;
import com.kumbu.backend.repository.ProductViewEventRepository;
import com.kumbu.backend.repository.UserRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final String NONE_ID = "__none__";

    private final CatalogProductRepository productRepository;
    private final ProductViewEventRepository viewEventRepository;
    private final UserRepository userRepository;
    private final CatalogService catalogService;
    private final SecurityUtils securityUtils;

    @Transactional
    public void recordView(CatalogProduct product, UUID userId) {
        User seller = product.getSellerId() != null
                ? userRepository.findById(product.getSellerId()).orElse(null) : null;

        viewEventRepository.save(ProductViewEvent.builder()
                .userId(userId)
                .productId(product.getId())
                .categoryId(product.getCategoryId())
                .city(seller != null ? seller.getCity() : null)
                .region(seller != null ? seller.getRegion() : null)
                .build());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.RECOMMENDATIONS, key = "'product:' + #productId + ':' + #limit")
    public Map<String, Object> getProductRecommendations(String productId, int limit) {
        CatalogProduct product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> ApiException.notFound("Anúncio não encontrado"));

        User seller = resolveSeller(product);
        String city = seller != null ? seller.getCity() : null;
        String region = seller != null ? seller.getRegion() : null;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("product_id", productId);
        result.put("similar", similarProducts(product, limit));
        result.put("nearby", nearbyProducts(productId, city, region, product.getCategoryId(), limit));
        result.put("same_seller", sameSellerProducts(product, limit));
        result.put("popular_in_category", popularInCategory(product.getCategoryId(), productId, limit));
        return result;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.RECOMMENDATIONS, key = "'similar:' + #productId + ':' + #limit")
    public List<RecommendationItemResponse> similarProducts(String productId, int limit) {
        CatalogProduct product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> ApiException.notFound("Anúncio não encontrado"));
        return similarProducts(product, limit);
    }

    @Transactional(readOnly = true)
    public List<RecommendationItemResponse> nearby(String city, String region, String categoryId, int limit) {
        return nearbyProducts(NONE_ID, city, region, categoryId, limit);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.RECOMMENDATIONS, key = "'trending:' + (#categoryId ?: 'all') + ':' + #limit")
    public List<RecommendationItemResponse> trending(String categoryId, int limit) {
        return dedupeAndMap(
                productRepository.findTrending(categoryId, PageRequest.of(0, limit * 2)),
                limit,
                RecommendationReason.TRENDING,
                80);
    }

    @Transactional(readOnly = true)
    public List<RecommendationItemResponse> forYou(int limit) {
        Optional<User> userOpt = currentUserOptional();
        if (userOpt.isEmpty()) {
            return trending(null, limit);
        }

        User user = userOpt.get();
        Set<String> exclude = new HashSet<>(user.getFavorites() != null ? user.getFavorites() : List.of());

        List<String> categoryIds = collectInterestCategories(user, exclude);
        if (categoryIds.isEmpty()) {
            categoryIds = List.of("eletronicos", "moda", "telemoveis", "servicos");
        }

        List<String> excludeList = exclude.isEmpty() ? List.of(NONE_ID) : new ArrayList<>(exclude);

        List<CatalogProduct> products = productRepository.findForYou(
                categoryIds,
                excludeList,
                user.getCity(),
                user.getRegion(),
                PageRequest.of(0, limit * 2));

        return dedupeAndMap(products, limit, RecommendationReason.FOR_YOU, 90);
    }

    @Transactional(readOnly = true)
    public List<RecommendationItemResponse> recentlyViewed(int limit) {
        UUID userId = securityUtils.currentUserId();
        List<ProductViewEvent> events = viewEventRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(0, limit * 3));

        LinkedHashSet<String> orderedIds = new LinkedHashSet<>();
        for (ProductViewEvent e : events) {
            orderedIds.add(e.getProductId());
            if (orderedIds.size() >= limit) break;
        }

        if (orderedIds.isEmpty()) return List.of();

        Map<String, CatalogProduct> byId = productRepository.findByIdInAndDeletedAtIsNull(new ArrayList<>(orderedIds))
                .stream().collect(Collectors.toMap(CatalogProduct::getId, p -> p));

        List<RecommendationItemResponse> items = new ArrayList<>();
        for (String id : orderedIds) {
            CatalogProduct p = byId.get(id);
            if (p != null) {
                items.add(toItem(p, RecommendationReason.RECENTLY_VIEWED, 70));
            }
        }
        return items;
    }

    @Transactional(readOnly = true)
    public List<RecommendationItemResponse> newNearby(int limit) {
        String city = currentUserOptional().map(User::getCity).orElse(null);
        Instant since = Instant.now().minus(14, ChronoUnit.DAYS);
        return dedupeAndMap(
                productRepository.findNewNearby(city, since, PageRequest.of(0, limit * 2)),
                limit,
                RecommendationReason.NEW_NEARBY,
                75);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.RECOMMENDATIONS, key = "'home:' + #limit + ':' + @securityUtils.cacheUserKey()")
    public Map<String, Object> homeFeed(int limit) {
        Map<String, Object> feed = new LinkedHashMap<>();
        Optional<User> user = currentUserOptional();
        String city = user.map(User::getCity).orElse(null);

        feed.put("for_you", forYou(limit));
        feed.put("trending", trending(null, Math.min(limit, 12)));
        feed.put("new_nearby", newNearby(Math.min(limit, 12)));
        feed.put("nearby", nearby(city, user.map(User::getRegion).orElse(null), null, Math.min(limit, 12)));
        if (user.isPresent()) {
            feed.put("recently_viewed", recentlyViewed(Math.min(limit, 8)));
        }
        feed.put("engine", "sql_spring_local");
        feed.put("note", "Recomendações por localização, similaridade e comportamento — sem APIs pagas");
        return feed;
    }

    private List<RecommendationItemResponse> similarProducts(CatalogProduct product, int limit) {
        String keyword = extractKeyword(product.getTitle());
        return dedupeAndMap(
                productRepository.findSimilar(
                        product.getId(),
                        product.getCategoryId(),
                        product.getSubcategoryId(),
                        keyword,
                        PageRequest.of(0, limit * 2)),
                limit,
                RecommendationReason.SIMILAR,
                85);
    }

    private List<RecommendationItemResponse> nearbyProducts(
            String excludeId, String city, String region, String categoryId, int limit) {
        List<CatalogProduct> merged = new ArrayList<>();

        if (city != null && !city.isBlank()) {
            merged.addAll(productRepository.findNearbyByCity(
                    excludeId, city.trim(), categoryId, PageRequest.of(0, limit)));
        }
        if (merged.size() < limit && region != null && !region.isBlank()) {
            merged.addAll(productRepository.findNearbyByRegion(
                    excludeId, region.trim(), categoryId, PageRequest.of(0, limit)));
        }

        RecommendationReason reason = city != null && !city.isBlank()
                ? RecommendationReason.NEARBY_CITY
                : RecommendationReason.NEARBY_REGION;

        return dedupeAndMap(merged, limit, reason, 80);
    }

    private List<RecommendationItemResponse> sameSellerProducts(CatalogProduct product, int limit) {
        if (product.getSellerId() == null) return List.of();
        return dedupeAndMap(
                productRepository.findOtherBySeller(
                        product.getSellerId(), product.getId(), PageRequest.of(0, limit)),
                limit,
                RecommendationReason.SAME_SELLER,
                60);
    }

    private List<RecommendationItemResponse> popularInCategory(String categoryId, String excludeId, int limit) {
        return dedupeAndMap(
                productRepository.findTrending(categoryId, PageRequest.of(0, limit * 2)).stream()
                        .filter(p -> !p.getId().equals(excludeId))
                        .toList(),
                limit,
                RecommendationReason.POPULAR,
                70);
    }

    private List<String> collectInterestCategories(User user, Set<String> exclude) {
        LinkedHashSet<String> categories = new LinkedHashSet<>();

        for (Object[] row : viewEventRepository.countCategoriesByUser(user.getId(), PageRequest.of(0, 5))) {
            categories.add((String) row[0]);
        }

        if (user.getFavorites() != null) {
            for (String favId : user.getFavorites()) {
                productRepository.findByIdAndDeletedAtIsNull(favId)
                        .ifPresent(p -> categories.add(p.getCategoryId()));
            }
        }

        return new ArrayList<>(categories);
    }

    private List<RecommendationItemResponse> dedupeAndMap(
            List<CatalogProduct> products, int limit, RecommendationReason reason, int baseScore) {
        LinkedHashMap<String, CatalogProduct> unique = new LinkedHashMap<>();
        for (CatalogProduct p : products) {
            unique.putIfAbsent(p.getId(), p);
        }

        List<RecommendationItemResponse> items = new ArrayList<>();
        int rank = 0;
        for (CatalogProduct p : unique.values()) {
            if (items.size() >= limit) break;
            items.add(toItem(p, reason, baseScore - rank));
            rank++;
        }
        return items;
    }

    private RecommendationItemResponse toItem(CatalogProduct p, RecommendationReason reason, int score) {
        ListingResponse listing = catalogService.toListingResponse(p);
        return RecommendationItemResponse.builder()
                .listing(listing)
                .reason(reason.name())
                .reasonLabel(reasonLabel(reason, listing.getCity()))
                .score(score)
                .build();
    }

    static String reasonLabel(RecommendationReason reason, String city) {
        return switch (reason) {
            case SIMILAR -> "Produtos semelhantes";
            case NEARBY_CITY -> city != null ? "Perto de ti em " + city : "Perto de ti";
            case NEARBY_REGION -> "Na tua região";
            case SAME_SELLER -> "Mais deste vendedor";
            case TRENDING -> "Em alta agora";
            case POPULAR -> "Populares na categoria";
            case FOR_YOU -> "Recomendado para ti";
            case RECENTLY_VIEWED -> "Viste recentemente";
            case SAME_CATEGORY -> "Mesma categoria";
            case NEW_NEARBY -> city != null ? "Novidades em " + city : "Novidades perto de ti";
            case FAVORITES_CATEGORY -> "Com base nos teus favoritos";
        };
    }

    static String extractKeyword(String title) {
        if (title == null || title.isBlank()) return null;
        String[] stopWords = {"de", "da", "do", "das", "dos", "em", "no", "na", "para", "com", "e", "o", "a"};
        Set<String> stops = Set.of(stopWords);
        for (String word : title.toLowerCase().split("\\s+")) {
            String w = word.replaceAll("[^a-záàâãéêíóôõúç0-9]", "");
            if (w.length() >= 3 && !stops.contains(w)) {
                return w;
            }
        }
        return null;
    }

    private User resolveSeller(CatalogProduct product) {
        return product.getSellerId() != null
                ? userRepository.findById(product.getSellerId()).orElse(null) : null;
    }

    private Optional<User> currentUserOptional() {
        try {
            return userRepository.findById(securityUtils.currentUserId());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
