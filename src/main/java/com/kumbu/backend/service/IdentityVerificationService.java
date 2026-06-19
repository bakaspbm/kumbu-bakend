package com.kumbu.backend.service;

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
    private static final String IDENTITY_TIER = "identity_kyc";

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
        doc.setReviewStatus("PENDING");
        doc.setRejectionReason(null);
        doc.setReviewedAt(null);
        doc.setReviewedBy(null);
        documentRepository.save(doc);

        verificationRepository
                .findFirstByUserIdAndTierOrderByCreatedAtDesc(userId, IDENTITY_TIER)
                .filter(v -> "REJECTED".equalsIgnoreCase(v.getStatus()))
                .ifPresent(verification -> {
                    verification.setStatus("PENDING");
                    verification.setAdminNote(null);
                    verification.setReviewedAt(null);
                    verification.setReviewedBy(null);
                    verificationRepository.save(verification);
                });

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
        List<UserIdentityDocument> docs = documentRepository.findByUserIdOrderBySideAsc(userId);
        if (docs.size() < 3) {
            throw ApiException.badRequest("Envie frente, verso e selfie antes de submeter.");
        }
        if (docs.stream().anyMatch(d -> "REJECTED".equalsIgnoreCase(d.getReviewStatus()))) {
            throw ApiException.badRequest("Substitua os documentos rejeitados antes de reenviar.");
        }

        SellerVerification verification = verificationRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(v -> IDENTITY_TIER.equals(v.getTier()))
                .filter(v -> "PENDING".equalsIgnoreCase(v.getStatus()) || "REJECTED".equalsIgnoreCase(v.getStatus()))
                .orElseGet(() -> SellerVerification.builder()
                        .userId(userId)
                        .tier(IDENTITY_TIER)
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
        Map<String, Map<String, Object>> documentReviews = new LinkedHashMap<>();

        for (String side : List.of("front", "back", "selfie")) {
            UserIdentityDocument doc = docs.stream()
                    .filter(d -> side.equals(d.getSide()))
                    .findFirst()
                    .orElse(null);
            uploaded.put(side, doc != null);
            if (doc != null) {
                Map<String, Object> review = new LinkedHashMap<>();
                review.put("status", doc.getReviewStatus() != null ? doc.getReviewStatus() : "PENDING");
                review.put("rejection_reason", doc.getRejectionReason());
                documentReviews.put(side, review);
            }
        }

        SellerVerification verification = verificationRepository
                .findFirstByUserIdAndTierOrderByCreatedAtDesc(userId, IDENTITY_TIER)
                .orElse(null);

        String reviewStatus = resolveReviewStatus(verification, docs);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("uploaded", uploaded);
        result.put("complete", uploaded.values().stream().allMatch(Boolean::booleanValue));
        result.put("reviewStatus", reviewStatus);
        result.put("adminNote", verification != null ? verification.getAdminNote() : null);
        result.put("documentReviews", documentReviews);
        return result;
    }

    private static String resolveReviewStatus(SellerVerification verification, List<UserIdentityDocument> docs) {
        if (verification != null) {
            return verification.getStatus();
        }
        if (docs.isEmpty()) {
            return "NOT_SUBMITTED";
        }
        if (docs.size() < 3) {
            return "INCOMPLETE";
        }
        boolean anyRejected = docs.stream().anyMatch(d -> "REJECTED".equalsIgnoreCase(d.getReviewStatus()));
        if (anyRejected) {
            return "REJECTED";
        }
        boolean allApproved = docs.stream().allMatch(d -> "APPROVED".equalsIgnoreCase(d.getReviewStatus()));
        if (allApproved) {
            return "APPROVED";
        }
        return "PENDING";
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
