package com.kumbu.backend.controller;

import com.kumbu.backend.dto.catalog.CategoryResponse;
import com.kumbu.backend.dto.catalog.CreateListingRequest;
import com.kumbu.backend.dto.catalog.ListingResponse;
import com.kumbu.backend.dto.catalog.UpdateListingRequest;
import com.kumbu.backend.dto.common.PageResponse;
import com.kumbu.backend.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping("/favorites")
    public PageResponse<ListingResponse> favorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalogService.listFavorites(page, size);
    }

    @GetMapping("/categories")
    public List<CategoryResponse> listCategories() {
        return catalogService.listCategories();
    }

    @GetMapping("/categories/{categoryId}/subcategories")
    public List<com.kumbu.backend.dto.catalog.SubcategoryResponse> listSubcategories(
            @PathVariable String categoryId) {
        return catalogService.listSubcategories(categoryId);
    }

    @GetMapping("/listings")
    public PageResponse<ListingResponse> search(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String subcategoryId,
            @RequestParam(required = false) java.util.UUID sellerId,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalogService.search(categoryId, subcategoryId, sellerId, featured, sort, q, page, size);
    }

    @GetMapping("/listings/featured")
    public PageResponse<ListingResponse> featured(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalogService.listFeatured(page, size);
    }

    @GetMapping("/listings/{id}")
    public ListingResponse getListing(@PathVariable String id) {
        return catalogService.getListing(id);
    }

    @PostMapping("/listings/{id}/view")
    public Map<String, Object> recordView(@PathVariable String id) {
        return catalogService.recordListingView(id);
    }

    @GetMapping("/listings/{id}/recommendations")
    public Map<String, Object> listingRecommendations(
            @PathVariable String id,
            @RequestParam(defaultValue = "12") int limit) {
        return catalogService.getListingRecommendations(id, limit);
    }

    @PostMapping("/listings")
    @ResponseStatus(HttpStatus.CREATED)
    public ListingResponse createListing(@Valid @RequestBody CreateListingRequest request) {
        return catalogService.createListing(request);
    }

    @GetMapping("/my-listings")
    public List<ListingResponse> myListings() {
        return catalogService.listMyListings();
    }

    @DeleteMapping("/listings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteListing(@PathVariable String id) {
        catalogService.deleteMyListing(id);
    }

    @PatchMapping("/listings/{id}")
    public ListingResponse updateListing(
            @PathVariable String id,
            @RequestBody UpdateListingRequest request) {
        return catalogService.updateMyListing(id, request);
    }
}
