package com.kumbu.backend.controller;

import com.kumbu.backend.dto.compliance.RecordConsentRequest;
import com.kumbu.backend.dto.compliance.SubmitReportRequest;
import com.kumbu.backend.service.ComplianceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
@Validated
public class ComplianceController {

    private final ComplianceService complianceService;

    @PostMapping("/consents")
    public Map<String, Object> recordConsent(@Valid @RequestBody RecordConsentRequest request) {
        return complianceService.recordConsent(request);
    }

    @PostMapping("/reports")
    public Map<String, Object> submitReport(@Valid @RequestBody SubmitReportRequest request) {
        return complianceService.submitReport(request);
    }
}
