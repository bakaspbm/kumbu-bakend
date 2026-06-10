package com.kumbu.backend.controller;



import com.kumbu.backend.dto.admin.AdminBroadcastNotificationRequest;

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

@RequestMapping("/api/v1/admin/notifications")

@RequiredArgsConstructor

@Validated

public class AdminNotificationsController {



    private final AdminManagementService adminManagementService;



    @GetMapping

    public Map<String, Object> list(

            @RequestParam(required = false) UUID user_id,

            @RequestParam(required = false) Integer limit,

            @RequestParam(defaultValue = "0") @Min(0) int page,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return adminManagementService.listNotifications(page, size, user_id, limit);

    }



    @PostMapping("/broadcast")

    public Map<String, Object> broadcast(@Valid @RequestBody AdminBroadcastNotificationRequest request) {

        return adminManagementService.broadcastNotification(AdminRequestMapper.toPayload(request));

    }



    @PostMapping("/{id}/hide")

    public Map<String, Object> hide(@PathVariable UUID id) {

        return adminManagementService.hideNotification(id);

    }



    @PostMapping("/{id}/unhide")

    public Map<String, Object> unhide(@PathVariable UUID id) {

        return adminManagementService.unhideNotification(id);

    }



    @PostMapping("/{id}/read")

    public Map<String, Object> markRead(@PathVariable UUID id) {

        return adminManagementService.markNotificationRead(id);

    }



    @DeleteMapping("/{id}")

    public void delete(@PathVariable UUID id) {

        adminManagementService.deleteNotification(id);

    }

}

