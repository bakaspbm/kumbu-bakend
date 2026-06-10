package com.kumbu.backend.controller;



import com.kumbu.backend.dto.admin.AdminBanUserRequest;
import com.kumbu.backend.dto.admin.AdminUserUpdateRequest;

import com.kumbu.backend.service.AdminManagementService;

import com.kumbu.backend.util.AdminRequestMapper;

import jakarta.validation.Valid;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.util.Map;

import java.util.UUID;



@RestController

@RequestMapping("/api/v1/admin/users")

@RequiredArgsConstructor

@Validated

public class AdminUsersController {



    private final AdminManagementService adminManagementService;



    @GetMapping

    public Map<String, Object> listUsers(

            @RequestParam(required = false) String q,

            @RequestParam(required = false) String status,

            @RequestParam(defaultValue = "0") @Min(0) int page,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return adminManagementService.searchUsers(q, status, page, size);

    }



    @GetMapping("/{id}")

    public Map<String, Object> getUser(@PathVariable UUID id) {

        return adminManagementService.getUser(id);

    }



    @PatchMapping("/{id}")

    public Map<String, Object> updateUser(@PathVariable UUID id, @Valid @RequestBody AdminUserUpdateRequest request) {

        return adminManagementService.updateUser(id, AdminRequestMapper.toPayload(request));

    }



    @DeleteMapping("/{id}")

    public void softDeleteUser(@PathVariable UUID id) {

        adminManagementService.softDeleteUser(id);

    }



    @PostMapping("/{id}/restore")

    public void restoreUser(@PathVariable UUID id) {

        adminManagementService.restoreUser(id);

    }

    @PostMapping("/{id}/ban")
    public Map<String, Object> banUser(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AdminBanUserRequest request) {
        AdminBanUserRequest body = request == null ? new AdminBanUserRequest() : request;
        return adminManagementService.banUser(id, body.getReason(), body.getUntil());
    }

    @PostMapping("/{id}/unban")
    public Map<String, Object> unbanUser(@PathVariable UUID id) {
        return adminManagementService.unbanUser(id);
    }

}

