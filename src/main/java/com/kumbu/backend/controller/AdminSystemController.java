package com.kumbu.backend.controller;



import com.kumbu.backend.dto.admin.AdminCreateAdminRequest;

import com.kumbu.backend.dto.admin.AdminCreateAuditRequest;

import com.kumbu.backend.dto.admin.AdminUpdateRoleRequest;

import com.kumbu.backend.service.AdminManagementService;

import jakarta.validation.Valid;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import com.kumbu.backend.security.AdminRoleExpressions;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.util.Map;

import java.util.UUID;



@RestController

@RequestMapping("/api/v1/admin/system")

@RequiredArgsConstructor

@Validated

public class AdminSystemController {



    private final AdminManagementService adminManagementService;



    @GetMapping("/admins")

    public Map<String, Object> listAdmins(

            @RequestParam(required = false) UUID user_id,

            @RequestParam(defaultValue = "0") @Min(0) int page,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return adminManagementService.listAdmins(page, size, user_id);

    }



    @PostMapping("/admins")

    @PreAuthorize(AdminRoleExpressions.SUPER_ADMIN)
    public Map<String, Object> createAdmin(@Valid @RequestBody AdminCreateAdminRequest request) {

        return adminManagementService.createAdmin(request.getUserId(), request.getRole());

    }



    @PatchMapping("/admins/{userId}")

    @PreAuthorize(AdminRoleExpressions.SUPER_ADMIN)
    public Map<String, Object> updateAdminRole(@PathVariable UUID userId, @Valid @RequestBody AdminUpdateRoleRequest request) {

        return adminManagementService.updateAdminRole(userId, request.getRole());

    }



    @DeleteMapping("/admins/{userId}")

    @PreAuthorize(AdminRoleExpressions.SUPER_ADMIN)
    public void deleteAdmin(@PathVariable UUID userId) {

        adminManagementService.deleteAdmin(userId);

    }



    @GetMapping("/audit-log")

    public Map<String, Object> listAuditLog(

            @RequestParam(required = false) String q,

            @RequestParam(required = false) String action,

            @RequestParam(required = false) @Min(1) @Max(200) Integer limit,

            @RequestParam(defaultValue = "0") @Min(0) int page,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return adminManagementService.listAuditLogs(q, action, limit, page, size);

    }



    @PostMapping("/audit-log")

    @PreAuthorize(AdminRoleExpressions.SUPER_ADMIN)
    public Map<String, Object> createAuditLog(@Valid @RequestBody AdminCreateAuditRequest request) {

        return adminManagementService.createAuditEntry(

                request.getAction(),

                request.getEntity(),

                request.getEntityId(),

                request.getPayload() == null ? Map.of() : request.getPayload()

        );

    }



    @GetMapping("/dashboard-overview")

    public Map<String, Object> dashboardOverview() {

        return adminManagementService.dashboardOverview();

    }



    @GetMapping("/analytics/signups")

    public Map<String, Object> analyticsSignups() {

        return adminManagementService.analyticsSignupsSeries();

    }



    @GetMapping("/analytics/demographics")

    public Map<String, Object> analyticsDemographics() {

        return adminManagementService.analyticsDemographicsBasic();

    }

    @GetMapping("/analytics/snapshot")
    public Map<String, Object> analyticsSnapshot(
            @RequestParam(defaultValue = "day") String period) {
        return adminManagementService.adminAnalyticsSnapshot(period);
    }

    @GetMapping("/analytics/rankings")
    public Map<String, Object> analyticsRankings(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return adminManagementService.adminAnalyticsRankings(limit);
    }

}

