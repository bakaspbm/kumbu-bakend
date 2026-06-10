package com.kumbu.backend.controller;

import com.kumbu.backend.dto.admin.AdminJobListingStatusRequest;
import com.kumbu.backend.service.AdminJobsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/jobs")
@RequiredArgsConstructor
@Validated
public class AdminJobsController {

    private final AdminJobsService adminJobsService;

    @GetMapping("/listings")
    public Map<String, Object> listJobListings(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminJobsService.listJobListings(status, q, page, size);
    }

    @PatchMapping("/listings/{id}/status")
    public Map<String, Object> updateJobListingStatus(
            @PathVariable @NotBlank @Size(max = 64) String id,
            @Valid @RequestBody AdminJobListingStatusRequest request) {
        return adminJobsService.updateJobListingStatus(id, request.getStatus());
    }

    @DeleteMapping("/listings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteJobListing(@PathVariable @NotBlank @Size(max = 64) String id) {
        adminJobsService.softDeleteJobListing(id);
    }

    @GetMapping("/applications")
    public Map<String, Object> listApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminJobsService.listApplications(status, page, size);
    }

    @GetMapping("/applications/pending-count")
    public Map<String, Object> pendingApplicationsCount() {
        return Map.of("count", adminJobsService.countPendingApplications());
    }

    @GetMapping("/applications/{id}")
    public Map<String, Object> getApplication(@PathVariable UUID id) {
        return adminJobsService.getApplication(id);
    }
}
