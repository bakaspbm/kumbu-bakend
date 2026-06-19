package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.SellerVerification;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.entity.UserIdentityDocument;
import com.kumbu.backend.domain.entity.UserNotification;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.SellerVerificationRepository;
import com.kumbu.backend.repository.UserIdentityDocumentRepository;
import com.kumbu.backend.repository.UserRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminIdentityService {

    private static final String IDENTITY_TIER = "identity_kyc";
    private static final Set<String> SIDES = Set.of("front", "back", "selfie");

    private static final Map<String, String> SIDE_LABELS = Map.of(
            "front", "Frente do documento",
            "back", "Verso do documento",
            "selfie", "Selfie com documento"
    );

    private final SellerVerificationRepository verificationRepository;
    private final UserIdentityDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Map<String, Object> listVerifications(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        Page<SellerVerification> result;
        if (status != null && !status.isBlank()) {
            result = verificationRepository.findByTierAndStatusOrderByCreatedAtDesc(
                    IDENTITY_TIER, status.trim().toUpperCase(), pageable);
        } else {
            result = verificationRepository.findByTierOrderByCreatedAtDesc(IDENTITY_TIER, pageable);
        }

        List<Map<String, Object>> items = result.getContent().stream()
                .map(this::toListItem)
                .toList();

        return Map.of(
                "items", items,
                "page", result.getNumber(),
                "size", result.getSize(),
                "total", result.getTotalElements(),
                "total_pages", result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getVerification(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Utilizador não encontrado"));
        SellerVerification verification = findIdentityVerification(userId).orElse(null);
        List<UserIdentityDocument> docs = documentRepository.findByUserIdOrderBySideAsc(userId);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user_id", userId);
        map.put("user_name", user.getDisplayName());
        map.put("user_email", user.getEmail());
        map.put("verification_id", verification != null ? verification.getId() : null);
        map.put("status", resolveIdentityStatus(verification, docs));
        map.put("admin_note", verification != null ? verification.getAdminNote() : null);
        map.put("reviewed_at", verification != null ? verification.getReviewedAt() : null);
        map.put("created_at", verification != null ? verification.getCreatedAt() : null);
        map.put("documents", docs.stream().map(this::toDocumentMap).toList());
        return map;
    }

    @Transactional(readOnly = true)
    public Resource loadDocument(UUID userId, String side) {
        UserIdentityDocument doc = requireDocument(userId, normalizeSide(side));
        Path path = storageService.resolvePrivatePath(doc.getStoragePath());
        return new FileSystemResource(path);
    }

    @Transactional(readOnly = true)
    public MediaType documentMediaType(UUID userId, String side) {
        UserIdentityDocument doc = requireDocument(userId, normalizeSide(side));
        Path path = storageService.resolvePrivatePath(doc.getStoragePath());
        return MediaType.parseMediaType(storageService.probeContentType(path));
    }

    @Transactional
    public Map<String, Object> approveDocument(UUID userId, String side, String note) {
        SellerVerification verification = requireReviewableVerification(userId);
        UserIdentityDocument doc = requireDocument(userId, normalizeSide(side));
        if ("APPROVED".equalsIgnoreCase(doc.getReviewStatus())) {
            return getVerification(userId);
        }

        UUID reviewerId = securityUtils.currentUserId();
        doc.setReviewStatus("APPROVED");
        doc.setRejectionReason(null);
        doc.setReviewedAt(Instant.now());
        doc.setReviewedBy(reviewerId);
        documentRepository.save(doc);

        syncVerificationStatus(userId, verification, trimNote(note), false);
        return getVerification(userId);
    }

    @Transactional
    public Map<String, Object> rejectDocument(UUID userId, String side, String note) {
        if (note == null || note.isBlank()) {
            throw ApiException.badRequest("Indique o motivo da rejeição.");
        }
        SellerVerification verification = requireReviewableVerification(userId);
        String normalizedSide = normalizeSide(side);
        UserIdentityDocument doc = requireDocument(userId, normalizedSide);
        String reason = note.trim();

        UUID reviewerId = securityUtils.currentUserId();
        doc.setReviewStatus("REJECTED");
        doc.setRejectionReason(reason);
        doc.setReviewedAt(Instant.now());
        doc.setReviewedBy(reviewerId);
        documentRepository.save(doc);

        verification.setStatus("REJECTED");
        verification.setAdminNote(buildRejectionSummary(documentRepository.findByUserIdOrderBySideAsc(userId)));
        verification.setReviewedAt(Instant.now());
        verification.setReviewedBy(reviewerId);
        verificationRepository.save(verification);

        notifyDocumentRejection(userId, normalizedSide, reason);
        return getVerification(userId);
    }

    @Transactional
    public Map<String, Object> approve(UUID userId, String note) {
        SellerVerification verification = requireReviewableVerification(userId);
        UUID reviewerId = securityUtils.currentUserId();
        Instant now = Instant.now();

        for (UserIdentityDocument doc : documentRepository.findByUserIdOrderBySideAsc(userId)) {
            if (!"APPROVED".equalsIgnoreCase(doc.getReviewStatus())) {
                doc.setReviewStatus("APPROVED");
                doc.setRejectionReason(null);
                doc.setReviewedAt(now);
                doc.setReviewedBy(reviewerId);
                documentRepository.save(doc);
            }
        }

        verification.setStatus("APPROVED");
        verification.setAdminNote(trimNote(note));
        verification.setReviewedAt(now);
        verification.setReviewedBy(reviewerId);
        verificationRepository.save(verification);

        notifyApproval(userId);
        return getVerification(userId);
    }

    @Transactional
    public Map<String, Object> reject(UUID userId, String note) {
        if (note == null || note.isBlank()) {
            throw ApiException.badRequest("Indique o motivo da rejeição.");
        }
        SellerVerification verification = requireReviewableVerification(userId);
        UUID reviewerId = securityUtils.currentUserId();
        Instant now = Instant.now();
        String reason = note.trim();

        for (UserIdentityDocument doc : documentRepository.findByUserIdOrderBySideAsc(userId)) {
            doc.setReviewStatus("REJECTED");
            doc.setRejectionReason(reason);
            doc.setReviewedAt(now);
            doc.setReviewedBy(reviewerId);
            documentRepository.save(doc);
        }

        verification.setStatus("REJECTED");
        verification.setAdminNote(reason);
        verification.setReviewedAt(now);
        verification.setReviewedBy(reviewerId);
        verificationRepository.save(verification);

        notifyFullRejection(userId, reason);
        return getVerification(userId);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return verificationRepository
                .findByTierAndStatusOrderByCreatedAtDesc(IDENTITY_TIER, "PENDING", PageRequest.of(0, 1))
                .getTotalElements();
    }

    private void syncVerificationStatus(
            UUID userId,
            SellerVerification verification,
            String optionalNote,
            boolean notifyOnPartialReject
    ) {
        List<UserIdentityDocument> docs = documentRepository.findByUserIdOrderBySideAsc(userId);
        if (docs.size() < 3) {
            return;
        }

        boolean allApproved = docs.stream().allMatch(d -> "APPROVED".equalsIgnoreCase(d.getReviewStatus()));
        boolean anyRejected = docs.stream().anyMatch(d -> "REJECTED".equalsIgnoreCase(d.getReviewStatus()));

        if (allApproved) {
            verification.setStatus("APPROVED");
            verification.setAdminNote(trimNote(optionalNote));
            verification.setReviewedAt(Instant.now());
            verification.setReviewedBy(securityUtils.currentUserId());
            verificationRepository.save(verification);
            notifyApproval(userId);
            return;
        }

        if (anyRejected) {
            verification.setStatus("REJECTED");
            verification.setAdminNote(buildRejectionSummary(docs));
            verification.setReviewedAt(Instant.now());
            verification.setReviewedBy(securityUtils.currentUserId());
            verificationRepository.save(verification);
            if (notifyOnPartialReject) {
                notifyFullRejection(userId, verification.getAdminNote());
            }
        } else {
            verification.setStatus("PENDING");
            verification.setAdminNote(null);
            verification.setReviewedAt(null);
            verification.setReviewedBy(null);
            verificationRepository.save(verification);
        }
    }

    private SellerVerification requireReviewableVerification(UUID userId) {
        SellerVerification verification = findIdentityVerification(userId)
                .orElseThrow(() -> ApiException.notFound("Pedido de verificação não encontrado"));
        if ("APPROVED".equalsIgnoreCase(verification.getStatus())) {
            throw ApiException.badRequest("Esta identidade já foi aprovada.");
        }
        if (documentRepository.countByUserId(userId) < 3) {
            throw ApiException.badRequest("Documentos incompletos.");
        }
        return verification;
    }

    private Optional<SellerVerification> findIdentityVerification(UUID userId) {
        return verificationRepository.findFirstByUserIdAndTierOrderByCreatedAtDesc(userId, IDENTITY_TIER);
    }

    private String resolveIdentityStatus(SellerVerification verification, List<UserIdentityDocument> docs) {
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

    private UserIdentityDocument requireDocument(UUID userId, String side) {
        return documentRepository
                .findById(new UserIdentityDocument.UserIdentityDocumentId(userId, side))
                .orElseThrow(() -> ApiException.notFound("Documento não encontrado"));
    }

    private Map<String, Object> toListItem(SellerVerification verification) {
        User user = userRepository.findById(verification.getUserId()).orElse(null);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", verification.getId());
        map.put("user_id", verification.getUserId());
        map.put("user_name", user != null ? user.getDisplayName() : null);
        map.put("user_email", user != null ? user.getEmail() : null);
        map.put("status", verification.getStatus());
        map.put("created_at", verification.getCreatedAt());
        map.put("reviewed_at", verification.getReviewedAt());
        map.put("documents_count", documentRepository.countByUserId(verification.getUserId()));
        return map;
    }

    private Map<String, Object> toDocumentMap(UserIdentityDocument doc) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("side", doc.getSide());
        map.put("uploaded_at", doc.getUploadedAt());
        map.put("review_status", doc.getReviewStatus() != null ? doc.getReviewStatus() : "PENDING");
        map.put("rejection_reason", doc.getRejectionReason());
        map.put("reviewed_at", doc.getReviewedAt());
        return map;
    }

    private String buildRejectionSummary(List<UserIdentityDocument> docs) {
        StringBuilder summary = new StringBuilder();
        for (UserIdentityDocument doc : docs) {
            if (!"REJECTED".equalsIgnoreCase(doc.getReviewStatus())) {
                continue;
            }
            if (summary.length() > 0) {
                summary.append("\n");
            }
            summary.append(SIDE_LABELS.getOrDefault(doc.getSide(), doc.getSide()))
                    .append(": ")
                    .append(doc.getRejectionReason() != null ? doc.getRejectionReason() : "Rejeitado");
        }
        return summary.toString();
    }

    private void notifyDocumentRejection(UUID userId, String side, String reason) {
        String label = SIDE_LABELS.getOrDefault(side, side);
        notificationService.saveAndPush(UserNotification.builder()
                .userId(userId)
                .title("Documento de identidade rejeitado")
                .body(label + ": " + reason + "\n\nSubstitua esta foto em Conta → Definições e reenvie o pedido.")
                .iconKey("badge_outlined")
                .actionUrl("/conta/definicoes")
                .build());
    }

    private void notifyFullRejection(UUID userId, String reason) {
        notificationService.saveAndPush(UserNotification.builder()
                .userId(userId)
                .title("Verificação de identidade rejeitada")
                .body(reason + "\n\nCorrija os documentos indicados em Conta → Definições e reenvie o pedido.")
                .iconKey("badge_outlined")
                .actionUrl("/conta/definicoes")
                .build());
    }

    private void notifyApproval(UUID userId) {
        notificationService.saveAndPush(UserNotification.builder()
                .userId(userId)
                .title("Identidade verificada")
                .body("A sua verificação de identidade foi aprovada. Obrigado!")
                .iconKey("verified_outlined")
                .actionUrl("/conta/definicoes")
                .build());
    }

    private static String normalizeSide(String side) {
        if (side == null) {
            throw ApiException.badRequest("Lado inválido");
        }
        String normalized = side.trim().toLowerCase();
        if (!SIDES.contains(normalized)) {
            throw ApiException.badRequest("Lado inválido");
        }
        return normalized;
    }

    private static String trimNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
