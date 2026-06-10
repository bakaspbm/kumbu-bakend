package com.kumbu.backend.controller;



import com.kumbu.backend.dto.admin.AdminProductUpdateRequest;

import com.kumbu.backend.dto.admin.AdminToggleRequest;

import com.kumbu.backend.service.AdminManagementService;

import com.kumbu.backend.util.AdminRequestMapper;

import jakarta.validation.Valid;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.util.Map;
import java.util.UUID;



@RestController

@RequestMapping("/api/v1/admin/products")

@RequiredArgsConstructor

@Validated

public class AdminProductsController {



    private final AdminManagementService adminManagementService;



    @GetMapping
    public Map<String, Object> listProducts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID seller_id,
            @RequestParam(required = false) String category_id,
            @RequestParam(required = false) Boolean out_of_stock,
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminManagementService.listProductsAdmin(
                q, seller_id, category_id, out_of_stock, deleted, page, size);
    }

    @GetMapping("/{id}")

    public Map<String, Object> getProduct(@PathVariable @NotBlank @Size(max = 64) String id) {

        return adminManagementService.getProduct(id);

    }



    @PatchMapping("/{id}")

    public Map<String, Object> updateProduct(

            @PathVariable @NotBlank @Size(max = 64) String id,

            @Valid @RequestBody AdminProductUpdateRequest request) {

        return adminManagementService.updateProduct(id, AdminRequestMapper.toPayload(request));

    }



    @DeleteMapping("/{id}")

    public void softDeleteProduct(@PathVariable @NotBlank @Size(max = 64) String id) {

        adminManagementService.softDeleteProduct(id);

    }



    @PostMapping("/{id}/featured")

    public Map<String, Object> toggleFeatured(

            @PathVariable @NotBlank @Size(max = 64) String id,

            @Valid @RequestBody AdminToggleRequest request) {

        return adminManagementService.toggleFeatured(id, request.getValue());

    }



    @PostMapping("/{id}/out-of-stock")

    public Map<String, Object> toggleOutOfStock(

            @PathVariable @NotBlank @Size(max = 64) String id,

            @Valid @RequestBody AdminToggleRequest request) {

        return adminManagementService.toggleOutOfStock(id, request.getValue());

    }

}

