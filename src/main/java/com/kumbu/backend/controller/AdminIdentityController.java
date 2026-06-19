package com.kumbu.backend.controller;

import com.kumbu.backend.dto.admin.AdminIdentityDocumentReviewRequest;
import com.kumbu.backend.dto.admin.AdminIdentityReviewRequest;
import com.kumbu.backend.service.AdminIdentityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/identity")
@RequiredArgsConstructor
@Validated
public class AdminIdentityController {

    private final AdminIdentityService adminIdentityService;

    @GetMapping("/verifications")
    public Map<String, Object> listVerifications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminIdentityService.listVerifications(status, page, size);
    }

    @GetMapping("/verifications/pending-count")
    public Map<String, Object> pendingCount() {
        return Map.of("count", adminIdentityService.countPending());
    }

    @GetMapping("/users/{userId}")
    public Map<String, Object> getVerification(@PathVariable UUID userId) {
        return adminIdentityService.getVerification(userId);
    }

    @GetMapping("/users/{userId}/documents/{side}")
    public ResponseEntity<Resource> document(@PathVariable UUID userId, @PathVariable String side) {
        Resource resource = adminIdentityService.loadDocument(userId, side);
        MediaType mediaType = adminIdentityService.documentMediaType(userId, side);
        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }

    @PostMapping("/users/{userId}/approve")
    public Map<String, Object> approve(
            @PathVariable UUID userId,
            @Valid @RequestBody(required = false) AdminIdentityReviewRequest request) {
        String note = request != null ? request.getNote() : null;
        return adminIdentityService.approve(userId, note);
    }

    @PostMapping("/users/{userId}/reject")
    public Map<String, Object> reject(
            @PathVariable UUID userId,
            @Valid @RequestBody AdminIdentityReviewRequest request) {
        return adminIdentityService.reject(userId, request.getNote());
    }

    @PostMapping("/users/{userId}/documents/{side}/approve")
    public Map<String, Object> approveDocument(
            @PathVariable UUID userId,
            @PathVariable String side,
            @Valid @RequestBody(required = false) AdminIdentityReviewRequest request) {
        String note = request != null ? request.getNote() : null;
        return adminIdentityService.approveDocument(userId, side, note);
    }

    @PostMapping("/users/{userId}/documents/{side}/reject")
    public Map<String, Object> rejectDocument(
            @PathVariable UUID userId,
            @PathVariable String side,
            @Valid @RequestBody AdminIdentityDocumentReviewRequest request) {
        return adminIdentityService.rejectDocument(userId, side, request.getNote());
    }
}
