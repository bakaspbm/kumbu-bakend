package com.kumbu.backend.controller;

import com.kumbu.backend.dto.recommendation.RecommendationItemResponse;
import com.kumbu.backend.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /** Feed principal da home — combina várias fontes */
    @GetMapping("/home")
    public Map<String, Object> homeFeed(
            @RequestParam(defaultValue = "16") int limit) {
        return recommendationService.homeFeed(Math.min(limit, 40));
    }

    /** Para ti — categorias favoritas, histórico, localização */
    @GetMapping("/for-you")
    public List<RecommendationItemResponse> forYou(
            @RequestParam(defaultValue = "20") int limit) {
        return recommendationService.forYou(Math.min(limit, 40));
    }

    /** Produtos semelhantes (mesma categoria, subcategoria, título) */
    @GetMapping("/similar/{productId}")
    public List<RecommendationItemResponse> similar(
            @PathVariable String productId,
            @RequestParam(defaultValue = "12") int limit) {
        return recommendationService.similarProducts(productId, Math.min(limit, 30));
    }

    /** Pacote completo para página de detalhe do anúncio */
    @GetMapping("/product/{productId}")
    public Map<String, Object> productBundle(
            @PathVariable String productId,
            @RequestParam(defaultValue = "12") int limit) {
        return recommendationService.getProductRecommendations(productId, Math.min(limit, 30));
    }

    /** Perto de ti — cidade e região do vendedor/comprador */
    @GetMapping("/nearby")
    public List<RecommendationItemResponse> nearby(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "20") int limit) {
        return recommendationService.nearby(city, region, categoryId, Math.min(limit, 40));
    }

    /** Em alta — mais visualizações + boost */
    @GetMapping("/trending")
    public List<RecommendationItemResponse> trending(
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "20") int limit) {
        return recommendationService.trending(categoryId, Math.min(limit, 40));
    }

    /** Novidades perto de ti (últimos 14 dias) */
    @GetMapping("/new-nearby")
    public List<RecommendationItemResponse> newNearby(
            @RequestParam(defaultValue = "12") int limit) {
        return recommendationService.newNearby(Math.min(limit, 30));
    }

    /** Histórico de visualizações (requer login) */
    @GetMapping("/recently-viewed")
    public List<RecommendationItemResponse> recentlyViewed(
            @RequestParam(defaultValue = "12") int limit) {
        return recommendationService.recentlyViewed(Math.min(limit, 30));
    }
}
