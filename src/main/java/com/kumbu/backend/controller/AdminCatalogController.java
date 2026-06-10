package com.kumbu.backend.controller;



import com.kumbu.backend.dto.admin.AdminCategoryCreateRequest;

import com.kumbu.backend.dto.admin.AdminCategoryUpdateRequest;

import com.kumbu.backend.dto.admin.AdminSubcategoryCreateRequest;

import com.kumbu.backend.dto.admin.AdminSubcategoryUpdateRequest;

import com.kumbu.backend.service.AdminManagementService;

import com.kumbu.backend.util.AdminRequestMapper;

import jakarta.validation.Valid;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.util.List;

import java.util.Map;



@RestController

@RequestMapping("/api/v1/admin/catalog")

@RequiredArgsConstructor

@Validated

public class AdminCatalogController {



    private final AdminManagementService adminManagementService;



    @GetMapping("/categories")

    public List<Map<String, Object>> listCategories() {

        return adminManagementService.listCategories();

    }



    @GetMapping("/subcategories")

    public List<Map<String, Object>> listAllSubcategories() {

        return adminManagementService.listAllSubcategories();

    }



    @PostMapping("/categories")

    public Map<String, Object> createCategory(@Valid @RequestBody AdminCategoryCreateRequest request) {

        return adminManagementService.createCategory(AdminRequestMapper.toPayload(request));

    }



    @PatchMapping("/categories/{id}")

    public Map<String, Object> updateCategory(

            @PathVariable @NotBlank @Size(max = 64) String id,

            @Valid @RequestBody AdminCategoryUpdateRequest request) {

        return adminManagementService.updateCategory(id, AdminRequestMapper.toPayload(request));

    }



    @DeleteMapping("/categories/{id}")

    public void deleteCategory(@PathVariable @NotBlank @Size(max = 64) String id) {

        adminManagementService.deleteCategory(id);

    }



    @GetMapping("/categories/{categoryId}/subcategories")

    public List<Map<String, Object>> listSubcategories(@PathVariable @NotBlank @Size(max = 64) String categoryId) {

        return adminManagementService.listSubcategories(categoryId);

    }



    @PostMapping("/categories/{categoryId}/subcategories")

    public Map<String, Object> createSubcategory(

            @PathVariable @NotBlank @Size(max = 64) String categoryId,

            @Valid @RequestBody AdminSubcategoryCreateRequest request) {

        return adminManagementService.createSubcategory(categoryId, AdminRequestMapper.toPayload(request));

    }



    @PatchMapping("/categories/{categoryId}/subcategories/{subcategoryId}")

    public Map<String, Object> updateSubcategory(

            @PathVariable @NotBlank @Size(max = 64) String categoryId,

            @PathVariable @NotBlank @Size(max = 64) String subcategoryId,

            @Valid @RequestBody AdminSubcategoryUpdateRequest request) {

        return adminManagementService.updateSubcategory(categoryId, subcategoryId, AdminRequestMapper.toPayload(request));

    }



    @DeleteMapping("/categories/{categoryId}/subcategories/{subcategoryId}")

    public void deleteSubcategory(

            @PathVariable @NotBlank @Size(max = 64) String categoryId,

            @PathVariable @NotBlank @Size(max = 64) String subcategoryId) {

        adminManagementService.deleteSubcategory(categoryId, subcategoryId);

    }

    
}

