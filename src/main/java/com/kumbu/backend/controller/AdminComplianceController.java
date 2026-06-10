package com.kumbu.backend.controller;

import com.kumbu.backend.service.AdminManagementService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/compliance")
@RequiredArgsConstructor
@Validated
public class AdminComplianceController {

    private final AdminManagementService adminManagementService;

    @GetMapping("/consents")
    public Map<String, Object> listConsents(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "40") @Min(1) @Max(200) int size) {
        return adminManagementService.listConsentsAdmin(page, size);
    }
}
