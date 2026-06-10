package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.SellerVerification;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.entity.UserIdentityDocument;
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

    private final SellerVerificationRepository verificationRepository;
    private final UserIdentityDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final SecurityUtils securityUtils;

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
        SellerVerification verification = verificationRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(v -> IDENTITY_TIER.equals(v.getTier()))
                .orElse(null);
        List<UserIdentityDocument> docs = documentRepository.findByUserIdOrderBySideAsc(userId);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("user_id", userId);
        map.put("user_name", user.getDisplayName());
        map.put("user_email", user.getEmail());
        map.put("verification_id", verification != null ? verification.getId() : null);
        map.put("status", verification != null ? verification.getStatus() : "NOT_SUBMITTED");
        map.put("admin_note", verification != null ? verification.getAdminNote() : null);
        map.put("reviewed_at", verification != null ? verification.getReviewedAt() : null);
        map.put("created_at", verification != null ? verification.getCreatedAt() : null);
        map.put("documents", docs.stream().map(this::toDocumentMap).toList());
        return map;
    }

    @Transactional(readOnly = true)
    public Resource loadDocument(UUID userId, String side) {
        String normalizedSide = normalizeSide(side);
        UserIdentityDocument doc = documentRepository
                .findById(new UserIdentityDocument.UserIdentityDocumentId(userId, normalizedSide))
                .orElseThrow(() -> ApiException.notFound("Documento não encontrado"));
        Path path = storageService.resolvePrivatePath(doc.getStoragePath());
        return new FileSystemResource(path);
    }

    @Transactional(readOnly = true)
    public MediaType documentMediaType(UUID userId, String side) {
        String normalizedSide = normalizeSide(side);
        UserIdentityDocument doc = documentRepository
                .findById(new UserIdentityDocument.UserIdentityDocumentId(userId, normalizedSide))
                .orElseThrow(() -> ApiException.notFound("Documento não encontrado"));
        Path path = storageService.resolvePrivatePath(doc.getStoragePath());
        return MediaType.parseMediaType(storageService.probeContentType(path));
    }

    @Transactional
    public Map<String, Object> approve(UUID userId, String note) {
        SellerVerification verification = requirePendingOrRejected(userId);
        verification.setStatus("APPROVED");
        verification.setAdminNote(trimNote(note));
        verification.setReviewedAt(Instant.now());
        verification.setReviewedBy(securityUtils.currentUserId());
        verificationRepository.save(verification);
        return getVerification(userId);
    }

    @Transactional
    public Map<String, Object> reject(UUID userId, String note) {
        if (note == null || note.isBlank()) {
            throw ApiException.badRequest("Indique o motivo da rejeição.");
        }
        SellerVerification verification = requirePendingOrRejected(userId);
        verification.setStatus("REJECTED");
        verification.setAdminNote(note.trim());
        verification.setReviewedAt(Instant.now());
        verification.setReviewedBy(securityUtils.currentUserId());
        verificationRepository.save(verification);
        return getVerification(userId);
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return verificationRepository
                .findByTierAndStatusOrderByCreatedAtDesc(IDENTITY_TIER, "PENDING", PageRequest.of(0, 1))
                .getTotalElements();
    }

    private SellerVerification requirePendingOrRejected(UUID userId) {
        SellerVerification verification = verificationRepository
                .findFirstByUserIdOrderByCreatedAtDesc(userId)
                .filter(v -> IDENTITY_TIER.equals(v.getTier()))
                .orElseThrow(() -> ApiException.notFound("Pedido de verificação não encontrado"));
        if (!"PENDING".equalsIgnoreCase(verification.getStatus())
                && !"REJECTED".equalsIgnoreCase(verification.getStatus())) {
            throw ApiException.badRequest("Este pedido já foi revisto.");
        }
        if (documentRepository.countByUserId(userId) < 3) {
            throw ApiException.badRequest("Documentos incompletos.");
        }
        return verification;
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
        return map;
    }

    private static String normalizeSide(String side) {
        if (side == null) {
            throw ApiException.badRequest("Lado inválido");
        }
        String normalized = side.trim().toLowerCase();
        if (!Set.of("front", "back", "selfie").contains(normalized)) {
            throw ApiException.badRequest("Lado inválido");
        }
        return normalized;
    }

    private static String trimNote(String note) {
        return note == null ? null : note.trim();
    }
}
