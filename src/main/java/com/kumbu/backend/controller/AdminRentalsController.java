package com.kumbu.backend.controller;

import com.kumbu.backend.service.AdminRentalsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/rentals")
@RequiredArgsConstructor
@Validated
public class AdminRentalsController {

    private final AdminRentalsService adminRentalsService;

    @GetMapping
    public Map<String, Object> listRentals(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminRentalsService.listRentals(status, page, size);
    }

    @GetMapping("/pending-count")
    public Map<String, Object> pendingCount() {
        return Map.of("count", adminRentalsService.countPendingRentals());
    }

    @GetMapping("/{id}")
    public Map<String, Object> getRental(@PathVariable UUID id) {
        return adminRentalsService.getRental(id);
    }
}
