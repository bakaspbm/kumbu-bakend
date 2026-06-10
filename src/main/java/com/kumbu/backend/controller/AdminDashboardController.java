package com.kumbu.backend.controller;

import com.kumbu.backend.service.AdminManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminManagementService adminManagementService;

    @GetMapping
    public Map<String, Object> stats() {
        return adminManagementService.adminDashboardStats();
    }

    @GetMapping("/marketplace")
    public Map<String, Object> marketplace() {
        return adminManagementService.adminDashboardMarketplace();
    }

    @GetMapping("/control")
    public Map<String, Object> control() {
        return adminManagementService.adminControlOverview();
    }
}
