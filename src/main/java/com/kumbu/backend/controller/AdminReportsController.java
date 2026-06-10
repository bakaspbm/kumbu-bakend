package com.kumbu.backend.controller;



import com.kumbu.backend.dto.admin.AdminNotifyReportOutcomeRequest;
import com.kumbu.backend.dto.admin.AdminUpdateReportRequest;

import com.kumbu.backend.service.AdminManagementService;

import jakarta.validation.Valid;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.util.Map;

import java.util.UUID;



@RestController

@RequestMapping("/api/v1/admin/reports")

@RequiredArgsConstructor

@Validated

public class AdminReportsController {



    private final AdminManagementService adminManagementService;



    @GetMapping

    public Map<String, Object> list(

            @RequestParam(required = false) String status,

            @RequestParam(required = false) UUID reported_user_id,

            @RequestParam(required = false) UUID reporter_id,

            @RequestParam(defaultValue = "0") @Min(0) int page,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return adminManagementService.listReports(page, size, status, reported_user_id, reporter_id);

    }



    @GetMapping("/{id}")

    public Map<String, Object> get(@PathVariable UUID id) {

        return adminManagementService.getReport(id);

    }



    @PatchMapping("/{id}")

    public Map<String, Object> update(@PathVariable UUID id, @Valid @RequestBody AdminUpdateReportRequest request) {

        return adminManagementService.updateReportStatus(id, request.getStatus(), request.getAdminNotes());

    }



    @PostMapping("/{id}/notify-outcome")

    public Map<String, Object> notifyOutcome(

            @PathVariable UUID id,

            @Valid @RequestBody AdminNotifyReportOutcomeRequest request) {

        return adminManagementService.notifyReportOutcome(

                id,

                request.getStatus(),

                request.getAdminNote(),

                request.getTitle(),

                request.getBody());

    }

}

