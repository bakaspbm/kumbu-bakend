package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.ContentReport;
import com.kumbu.backend.domain.entity.UserConsent;
import com.kumbu.backend.dto.compliance.RecordConsentRequest;
import com.kumbu.backend.dto.compliance.SubmitReportRequest;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.ContentReportRepository;
import com.kumbu.backend.repository.UserConsentRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private static final Set<String> REPORT_REASONS = Set.of(
            "spam", "fraud", "illegal", "harassment", "misleading", "ip", "other"
    );

    private final UserConsentRepository consentRepository;
    private final ContentReportRepository reportRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public Map<String, Object> recordConsent(RecordConsentRequest request) {
        UUID userId = securityUtils.currentUserId();
        UserConsent saved = consentRepository.save(UserConsent.builder()
                .userId(userId)
                .consentType(request.getConsentType().trim())
                .userAgent(request.getUserAgent())
                .build());
        return Map.of(
                "id", saved.getId(),
                "consentType", saved.getConsentType(),
                "acceptedAt", saved.getAcceptedAt()
        );
    }

    @Transactional
    public Map<String, Object> submitReport(SubmitReportRequest request) {
        UUID reporterId = securityUtils.currentUserId();
        String reason = request.getReason().trim().toLowerCase();
        if (!REPORT_REASONS.contains(reason)) {
            throw ApiException.badRequest("Motivo de denúncia inválido");
        }

        String targetType = mapTargetType(request.getTargetType());
        String targetId = request.getTargetId().trim();
        if (targetId.isEmpty()) {
            throw ApiException.badRequest("Alvo da denúncia inválido");
        }

        UUID reportedUserId = request.getReportedUserId();
        if (reportedUserId != null && reportedUserId.equals(reporterId)) {
            throw ApiException.badRequest("Não pode denunciar-se a si próprio");
        }

        ContentReport saved = reportRepository.save(ContentReport.builder()
                .reporterId(reporterId)
                .targetType(targetType)
                .targetId(normalizeTargetId(request.getTargetType(), targetId))
                .reportedUserId(reportedUserId)
                .reason(reason)
                .details(trimToNull(request.getDetails()))
                .build());

        return Map.of("id", saved.getId());
    }

    private static String mapTargetType(String frontendType) {
        if (frontendType == null) {
            throw ApiException.badRequest("Tipo de alvo inválido");
        }
        return switch (frontendType.trim().toLowerCase()) {
            case "product", "listing" -> "listing";
            case "user" -> "user";
            case "message" -> "message";
            case "review" -> "listing";
            case "conversation" -> "conversation";
            default -> throw ApiException.badRequest("Tipo de alvo inválido");
        };
    }

    private static String normalizeTargetId(String frontendType, String targetId) {
        if (frontendType != null && "review".equalsIgnoreCase(frontendType.trim())) {
            return "review:" + targetId;
        }
        return targetId;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
