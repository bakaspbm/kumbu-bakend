package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.*;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.*;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountDataService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserDeletionEventRepository deletionEventRepository;
    private final UserConsentRepository consentRepository;
    private final UserCvRepository cvRepository;
    private final JobApplicationRepository applicationRepository;
    private final CatalogProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ConversationRepository conversationRepository;
    private final UserNotificationRepository notificationRepository;
    private final ContentReportRepository reportRepository;
    private final SecurityUtils securityUtils;
    private final ProfileCompletenessService profileCompletenessService;

    @Transactional(readOnly = true)
    public Map<String, Object> exportAccountData() {
        User user = getCurrentUser();
        UUID userId = user.getId();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exportedAt", Instant.now().toString());
        data.put("profile", toProfileMap(user));
        data.put("favorites", user.getFavorites());
        data.put("cart", user.getCart());
        data.put("listings", productRepository.findBySellerIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
                .map(this::toListingSummary)
                .toList());
        data.put("cvs", cvRepository.findByUserIdOrderByUpdatedAtDesc(userId));
        data.put("jobApplications", applicationRepository.findByApplicantIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toApplicationSummary)
                .toList());
        data.put("ordersPurchases", orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toOrderSummary)
                .toList());
        data.put("ordersSales", orderRepository.findBySellerIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toOrderSummary)
                .toList());
        data.put("conversations", conversationRepository
                .findByBuyerIdOrSellerIdOrderByUpdatedAtDesc(userId, userId).stream()
                .map(c -> toConversationSummary(c, userId))
                .toList());
        data.put("notifications", notificationRepository.findByUserIdAndHiddenAtIsNullOrderByCreatedAtDesc(userId)
                .stream()
                .limit(200)
                .map(this::toNotificationSummary)
                .toList());
        data.put("consents", consentRepository.findByUserIdOrderByAcceptedAtDesc(userId).stream()
                .map(c -> Map.of(
                        "consentType", c.getConsentType(),
                        "acceptedAt", c.getAcceptedAt()))
                .toList());
        data.put("reportsSubmitted", reportRepository.findByReporterIdOrderByCreatedAtDesc(userId).stream()
                .limit(100)
                .map(r -> Map.of(
                        "id", r.getId(),
                        "targetType", r.getTargetType(),
                        "targetId", r.getTargetId(),
                        "reason", r.getReason(),
                        "createdAt", r.getCreatedAt()))
                .toList());
        return data;
    }

    @Transactional
    public void deleteAccount() {
        User user = getCurrentUser();
        UUID userId = user.getId();
        Instant now = Instant.now();

        deletionEventRepository.save(UserDeletionEvent.builder()
                .userId(userId)
                .email(user.getEmail())
                .deletedAt(now)
                .source("app")
                .build());

        refreshTokenRepository.findByUserId(userId).forEach(token -> {
            token.setRevokedAt(now);
            refreshTokenRepository.save(token);
        });

        user.setDeletedAt(now);
        userRepository.save(user);
    }

    private User getCurrentUser() {
        UUID id = securityUtils.currentUserId();
        return userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> ApiException.notFound("Utilizador não encontrado"));
    }

    private Map<String, Object> toProfileMap(User user) {
        Map<String, Object> readiness = profileCompletenessService.assess(user);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("fullName", user.getDisplayName());
        profile.put("phone", user.getPhone());
        profile.put("city", user.getCity());
        profile.put("region", user.getRegion());
        profile.put("country", user.getCountry());
        profile.put("deliveryAddress", user.getDeliveryAddress());
        profile.put("profileComplete", readiness.get("profile_complete"));
        return profile;
    }

    private Map<String, Object> toListingSummary(CatalogProduct p) {
        return Map.of(
                "id", p.getId(),
                "title", p.getTitle(),
                "priceLabel", p.getPriceLabel(),
                "listingKind", p.getListingKind().name(),
                "createdAt", p.getCreatedAt());
    }

    private Map<String, Object> toApplicationSummary(JobApplication a) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", a.getId());
        row.put("jobId", a.getJobId());
        row.put("status", a.getStatus());
        row.put("createdAt", a.getCreatedAt());
        return row;
    }

    private Map<String, Object> toOrderSummary(Order o) {
        return Map.of(
                "id", o.getId(),
                "status", o.getStatus(),
                "totalLabel", o.getTotalLabel(),
                "createdAt", o.getCreatedAt());
    }

    private Map<String, Object> toConversationSummary(Conversation c, UUID userId) {
        return Map.of(
                "id", c.getId(),
                "productId", c.getProductId(),
                "role", userId.equals(c.getBuyerId()) ? "buyer" : "seller",
                "updatedAt", c.getUpdatedAt());
    }

    private Map<String, Object> toNotificationSummary(UserNotification n) {
        return Map.of(
                "id", n.getId(),
                "title", n.getTitle(),
                "body", n.getBody(),
                "readAt", n.getReadAt(),
                "createdAt", n.getCreatedAt());
    }
}
