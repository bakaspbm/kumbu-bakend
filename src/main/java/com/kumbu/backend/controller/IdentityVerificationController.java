package com.kumbu.backend.controller;

import com.kumbu.backend.service.IdentityVerificationService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/verification/identity")
@RequiredArgsConstructor
@Validated
public class IdentityVerificationController {

    private final IdentityVerificationService identityVerificationService;

    @GetMapping("/status")
    public Map<String, Object> status() {
        return identityVerificationService.getStatus();
    }

    @PostMapping("/{side}")
    public Map<String, Object> upload(
            @PathVariable String side,
            @RequestParam("file") @NotNull MultipartFile file) {
        return identityVerificationService.uploadDocument(side, file);
    }

    @PostMapping("/submit")
    public Map<String, Object> submit() {
        return identityVerificationService.submitForReview();
    }
}
