package com.kumbu.backend.controller;



import com.kumbu.backend.dto.admin.AdminLegalDocumentUpsertRequest;

import com.kumbu.backend.dto.admin.AdminMarketingBlockCreateRequest;

import com.kumbu.backend.dto.admin.AdminMarketingBlockUpdateRequest;

import com.kumbu.backend.dto.admin.AdminPaymentMethodCreateRequest;

import com.kumbu.backend.dto.admin.AdminPaymentMethodUpdateRequest;

import com.kumbu.backend.dto.admin.AdminSortFilterCreateRequest;

import com.kumbu.backend.dto.admin.AdminSortFilterUpdateRequest;

import com.kumbu.backend.dto.admin.AdminSupportSettingsUpsertRequest;

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

@RequestMapping("/api/v1/admin/app")

@RequiredArgsConstructor

@Validated

public class AdminAppController {



    private final AdminManagementService adminManagementService;



    @GetMapping("/marketing-blocks")

    public List<Map<String, Object>> listMarketingBlocks() {

        return adminManagementService.listMarketingBlocks();

    }



    @PostMapping("/marketing-blocks")

    public Map<String, Object> createMarketingBlock(@Valid @RequestBody AdminMarketingBlockCreateRequest request) {

        return adminManagementService.createMarketingBlock(AdminRequestMapper.toPayload(request));

    }



    @PatchMapping("/marketing-blocks/{id}")

    public Map<String, Object> updateMarketingBlock(

            @PathVariable @NotBlank @Size(max = 64) String id,

            @Valid @RequestBody AdminMarketingBlockUpdateRequest request) {

        return adminManagementService.updateMarketingBlock(id, AdminRequestMapper.toPayload(request));

    }



    @DeleteMapping("/marketing-blocks/{id}")

    public void deleteMarketingBlock(@PathVariable @NotBlank @Size(max = 64) String id) {

        adminManagementService.deleteMarketingBlock(id);

    }



    @GetMapping("/legal-documents")

    public List<Map<String, Object>> listLegalDocuments() {

        return adminManagementService.listLegalDocuments();

    }



    @GetMapping("/legal-documents/{slug}")

    public Map<String, Object> getLegalDocument(@PathVariable @NotBlank @Size(max = 64) String slug) {

        return adminManagementService.getLegalDocument(slug);

    }



    @PutMapping("/legal-documents/{slug}")

    public Map<String, Object> upsertLegalDocument(

            @PathVariable @NotBlank @Size(max = 64) String slug,

            @Valid @RequestBody AdminLegalDocumentUpsertRequest request) {

        return adminManagementService.upsertLegalDocument(slug, AdminRequestMapper.toPayload(request));

    }



    @PostMapping("/legal-documents/seed")

    public void seedLegalDocuments() {

        adminManagementService.seedLegalDocumentsDefaults();

    }



    @GetMapping("/sort-filters")

    public List<Map<String, Object>> listSortFilters() {

        return adminManagementService.listSortFilters();

    }



    @PostMapping("/sort-filters")

    public Map<String, Object> createSortFilter(@Valid @RequestBody AdminSortFilterCreateRequest request) {

        return adminManagementService.createSortFilter(AdminRequestMapper.toPayload(request));

    }



    @PatchMapping("/sort-filters/{id}")

    public Map<String, Object> updateSortFilter(

            @PathVariable @NotBlank @Size(max = 64) String id,

            @Valid @RequestBody AdminSortFilterUpdateRequest request) {

        return adminManagementService.updateSortFilter(id, AdminRequestMapper.toPayload(request));

    }



    @DeleteMapping("/sort-filters/{id}")

    public void deleteSortFilter(@PathVariable @NotBlank @Size(max = 64) String id) {

        adminManagementService.deleteSortFilter(id);

    }



    @GetMapping("/payment-methods")

    public List<Map<String, Object>> listPaymentMethods() {

        return adminManagementService.listPaymentMethods();

    }



    @PostMapping("/payment-methods")

    public Map<String, Object> createPaymentMethod(@Valid @RequestBody AdminPaymentMethodCreateRequest request) {

        return adminManagementService.createPaymentMethod(AdminRequestMapper.toPayload(request));

    }



    @PatchMapping("/payment-methods/{id}")

    public Map<String, Object> updatePaymentMethod(

            @PathVariable @NotBlank @Size(max = 64) String id,

            @Valid @RequestBody AdminPaymentMethodUpdateRequest request) {

        return adminManagementService.updatePaymentMethod(id, AdminRequestMapper.toPayload(request));

    }



    @DeleteMapping("/payment-methods/{id}")

    public void deletePaymentMethod(@PathVariable @NotBlank @Size(max = 64) String id) {

        adminManagementService.deletePaymentMethod(id);

    }



    @GetMapping("/support-settings")

    public Map<String, Object> getSupportSettings() {

        return adminManagementService.getSupportSettings();

    }



    @PutMapping("/support-settings")

    public Map<String, Object> upsertSupportSettings(@Valid @RequestBody AdminSupportSettingsUpsertRequest request) {

        return adminManagementService.upsertSupportSettings(AdminRequestMapper.toPayload(request));

    }

}

