package com.kumbu.backend.service;

import com.kumbu.backend.config.KumbuProperties;
import com.kumbu.backend.domain.entity.SellerVerification;
import com.kumbu.backend.domain.entity.UserIdentityDocument;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.SellerVerificationRepository;
import com.kumbu.backend.repository.UserIdentityDocumentRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdentityVerificationService {

    private static final Set<String> SIDES = Set.of("front", "back", "selfie");

    private final UserIdentityDocumentRepository documentRepository;
    private final SellerVerificationRepository verificationRepository;
    private final StorageService storageService;
    private final SecurityUtils securityUtils;

    @Transactional
    public Map<String, Object> uploadDocument(String side, MultipartFile file) {
        String normalizedSide = normalizeSide(side);
        UUID userId = securityUtils.currentUserId();
        String storagePath = storageService.storeIdentity(userId, normalizedSide, file);

        UserIdentityDocument doc = documentRepository
                .findById(new UserIdentityDocument.UserIdentityDocumentId(userId, normalizedSide))
                .orElse(UserIdentityDocument.builder()
                        .userId(userId)
                        .side(normalizedSide)
                        .build());
        doc.setStoragePath(storagePath);
        documentRepository.save(doc);

        Map<String, Object> status = buildStatus(userId);
        status.put("uploadedSide", normalizedSide);
        return status;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStatus() {
        return buildStatus(securityUtils.currentUserId());
    }

    @Transactional
    public Map<String, Object> submitForReview() {
        UUID userId = securityUtils.currentUserId();
        if (documentRepository.countByUserId(userId) < 3) {
            throw ApiException.badRequest("Envie frente, verso e selfie antes de submeter.");
        }

        SellerVerification verification = verificationRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(v -> "PENDING".equalsIgnoreCase(v.getStatus()) || "REJECTED".equalsIgnoreCase(v.getStatus()))
                .orElseGet(() -> SellerVerification.builder()
                        .userId(userId)
                        .tier("identity_kyc")
                        .status("PENDING")
                        .build());
        verification.setStatus("PENDING");
        verification.setAdminNote(null);
        verification.setReviewedAt(null);
        verification.setReviewedBy(null);
        verificationRepository.save(verification);

        return buildStatus(userId);
    }

    private Map<String, Object> buildStatus(UUID userId) {
        List<UserIdentityDocument> docs = documentRepository.findByUserIdOrderBySideAsc(userId);
        Map<String, Boolean> uploaded = new LinkedHashMap<>();
        for (String side : List.of("front", "back", "selfie")) {
            uploaded.put(side, docs.stream().anyMatch(d -> side.equals(d.getSide())));
        }

        String reviewStatus = verificationRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(SellerVerification::getStatus)
                .orElse("NOT_SUBMITTED");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("uploaded", uploaded);
        result.put("complete", uploaded.values().stream().allMatch(Boolean::booleanValue));
        result.put("reviewStatus", reviewStatus);
        return result;
    }

    private static String normalizeSide(String side) {
        if (side == null) {
            throw ApiException.badRequest("Lado do documento inválido");
        }
        String normalized = side.trim().toLowerCase();
        if (!SIDES.contains(normalized)) {
            throw ApiException.badRequest("Lado do documento inválido");
        }
        return normalized;
    }
}
