package com.kumbu.backend.service;

import com.kumbu.backend.config.CacheNames;
import com.kumbu.backend.domain.entity.AdminAuditLog;
import com.kumbu.backend.domain.entity.AdminUser;
import com.kumbu.backend.domain.entity.AppCategorySortFilter;
import com.kumbu.backend.domain.entity.AppMarketingBlock;
import com.kumbu.backend.domain.entity.AppPaymentMethod;
import com.kumbu.backend.domain.entity.AppSupportSettings;
import com.kumbu.backend.domain.entity.CatalogCategory;
import com.kumbu.backend.domain.entity.CatalogProduct;
import com.kumbu.backend.domain.entity.CatalogSubcategory;
import com.kumbu.backend.domain.entity.CatalogSubcategoryId;
import com.kumbu.backend.domain.entity.ChatMessage;
import com.kumbu.backend.domain.entity.ContentReport;
import com.kumbu.backend.domain.entity.Conversation;
import com.kumbu.backend.domain.entity.LegalDocument;
import com.kumbu.backend.domain.entity.Order;
import com.kumbu.backend.domain.entity.ProductReview;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.entity.UserConsent;
import com.kumbu.backend.domain.entity.UserNotification;
import com.kumbu.backend.domain.enums.AdminRole;
import com.kumbu.backend.domain.enums.OrderStatus;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.AdminAuditLogRepository;
import com.kumbu.backend.repository.AdminUserRepository;
import com.kumbu.backend.repository.AppCategorySortFilterRepository;
import com.kumbu.backend.repository.AppMarketingBlockRepository;
import com.kumbu.backend.repository.AppPaymentMethodRepository;
import com.kumbu.backend.repository.AppSupportSettingsRepository;
import com.kumbu.backend.repository.CatalogCategoryRepository;
import com.kumbu.backend.repository.CatalogProductRepository;
import com.kumbu.backend.repository.CatalogSubcategoryRepository;
import com.kumbu.backend.repository.ChatMessageRepository;
import com.kumbu.backend.repository.ContentReportRepository;
import com.kumbu.backend.repository.ConversationRepository;
import com.kumbu.backend.repository.LegalDocumentRepository;
import com.kumbu.backend.repository.OrderRepository;
import com.kumbu.backend.repository.ProductReviewRepository;
import com.kumbu.backend.repository.UserConsentRepository;
import com.kumbu.backend.repository.UserNotificationRepository;
import com.kumbu.backend.repository.UserRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminManagementService {

    private static final List<String> REPORT_STATUSES = List.of("pending", "reviewing", "resolved", "dismissed");

    private final UserRepository userRepository;
    private final CatalogProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ContentReportRepository contentReportRepository;
    private final ProductReviewRepository productReviewRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final UserConsentRepository userConsentRepository;
    private final CatalogCategoryRepository categoryRepository;
    private final CatalogSubcategoryRepository subcategoryRepository;
    private final AppMarketingBlockRepository marketingBlockRepository;
    private final LegalDocumentRepository legalDocumentRepository;
    private final AppCategorySortFilterRepository sortFilterRepository;
    private final AppPaymentMethodRepository paymentMethodRepository;
    private final AppSupportSettingsRepository supportSettingsRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminAuditLogRepository auditLogRepository;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final MonetizationGateAlertService gateAlertService;
    private final AdminIdentityService adminIdentityService;
    private final AuthService authService;

    @Transactional(readOnly = true)
    public Map<String, Object> getUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        Map<String, Object> map = toUserMap(user);
        map.put("identity_verification", adminIdentityService.getVerification(userId));
        return map;
    }

    @Transactional
    public Map<String, Object> updateUser(UUID userId, Map<String, Object> payload) {
        requireNonEmpty(payload);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (payload.containsKey("display_name")) user.setDisplayName(asString(payload.get("display_name")));
        if (payload.containsKey("email")) user.setEmail(asString(payload.get("email")));
        if (payload.containsKey("phone")) user.setPhone(asString(payload.get("phone")));
        if (payload.containsKey("photo_url")) user.setPhotoUrl(asString(payload.get("photo_url")));
        if (payload.containsKey("city")) user.setCity(asString(payload.get("city")));
        if (payload.containsKey("region")) user.setRegion(asString(payload.get("region")));
        if (payload.containsKey("country")) user.setCountry(asString(payload.get("country")));
        if (payload.containsKey("gender")) user.setGender(asString(payload.get("gender")));
        if (payload.containsKey("birth_date")) user.setBirthDate(asLocalDate(payload.get("birth_date")));
        if (payload.containsKey("seller_verified")) {
            Object raw = payload.get("seller_verified");
            boolean verified = raw instanceof Boolean b ? b : "true".equalsIgnoreCase(String.valueOf(raw));
            user.setSellerVerified(verified);
        }

        User saved = userRepository.save(user);
        createAuditEntry("user.updated", "user", saved.getId().toString(), payload);
        return toUserMap(saved);
    }

    @Transactional
    public void softDeleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
        createAuditEntry("user.soft_deleted", "user", user.getId().toString(), Map.of("deleted_at", user.getDeletedAt()));
    }

    @Transactional
    public void restoreUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        user.setDeletedAt(null);
        userRepository.save(user);
        createAuditEntry("user.restored", "user", user.getId().toString(), Map.of());
    }

    @Transactional
    public Map<String, Object> banUser(UUID userId, String reason, Instant until) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        if (SupportService.SUPPORT_SYSTEM_USER_ID.equals(userId)) {
            throw ApiException.badRequest("Não pode banir a conta de suporte do sistema");
        }
        UUID adminId = securityUtils.currentUserId();
        user.setBannedAt(Instant.now());
        user.setBannedUntil(until);
        user.setBanReason(reason == null || reason.isBlank() ? "Suspensão administrativa" : reason.trim());
        user.setBannedBy(adminId);
        User saved = userRepository.save(user);
        authService.invalidateAllSessions(saved);
        createAuditEntry("user.banned", "user", saved.getId().toString(),
                Map.of("reason", saved.getBanReason(), "until", until));
        return toUserMap(saved);
    }

    @Transactional
    public Map<String, Object> unbanUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        user.setBannedAt(null);
        user.setBannedUntil(null);
        user.setBanReason(null);
        user.setBannedBy(null);
        User saved = userRepository.save(user);
        createAuditEntry("user.unbanned", "user", saved.getId().toString(), Map.of());
        return toUserMap(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> searchUsers(String q, String status, int page, int size) {
        Pageable pageable = pageRequest(page, size, "createdAt");
        String normalized = normalizeQuery(q);
        String normalizedStatus = status == null || status.isBlank() ? null : status.trim().toLowerCase();
        Page<User> users = (normalized == null && normalizedStatus == null)
                ? userRepository.findAll(pageable)
                : userRepository.searchAdmin(normalized, normalizedStatus, pageable);
        return pageMap(users.map(this::toUserMap), page, size);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProduct(String productId) {
        CatalogProduct product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        return toProductMap(product);
    }

    @Transactional
    public Map<String, Object> updateProduct(String productId, Map<String, Object> payload) {
        requireNonEmpty(payload);
        CatalogProduct product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));

        if (payload.containsKey("title")) product.setTitle(asString(payload.get("title")));
        if (payload.containsKey("category_id")) product.setCategoryId(asString(payload.get("category_id")));
        if (payload.containsKey("subcategory_id")) product.setSubcategoryId(asString(payload.get("subcategory_id")));
        if (payload.containsKey("price_label")) product.setPriceLabel(asString(payload.get("price_label")));
        if (payload.containsKey("old_price_label")) product.setOldPriceLabel(asString(payload.get("old_price_label")));
        if (payload.containsKey("delivery_text")) product.setDeliveryText(asString(payload.get("delivery_text")));
        if (payload.containsKey("description")) product.setDescription(asString(payload.get("description")));
        if (payload.containsKey("sort_order")) product.setSortOrder(asInt(payload.get("sort_order"), product.getSortOrder()));

        CatalogProduct saved = productRepository.save(product);
        createAuditEntry("product.updated", "product", saved.getId(), payload);
        return toProductMap(saved);
    }

    @Transactional
    public void softDeleteProduct(String productId) {
        CatalogProduct product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        product.setDeletedAt(Instant.now());
        productRepository.save(product);
        createAuditEntry("product.soft_deleted", "product", product.getId(), Map.of());
    }

    @Transactional
    public Map<String, Object> toggleFeatured(String productId, boolean featured) {
        CatalogProduct product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        product.setFeatured(featured);
        CatalogProduct saved = productRepository.save(product);
        createAuditEntry("product.featured_toggled", "product", saved.getId(), Map.of("is_featured", featured));
        return toProductMap(saved);
    }

    @Transactional
    public Map<String, Object> toggleOutOfStock(String productId, boolean outOfStock) {
        CatalogProduct product = productRepository.findById(productId)
                .orElseThrow(() -> ApiException.notFound("Product not found"));
        product.setOutOfStock(outOfStock);
        CatalogProduct saved = productRepository.save(product);
        createAuditEntry("product.stock_toggled", "product", saved.getId(), Map.of("is_out_of_stock", outOfStock));
        return toProductMap(saved);
    }

    @Transactional
    public Map<String, Object> updateOrderStatus(String orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        OrderStatus next = OrderStatus.valueOf(status.toUpperCase());
        order.setStatus(next);
        Order saved = orderRepository.save(order);
        createAuditEntry("order.status_updated", "order", saved.getId(), Map.of("status", next.name().toLowerCase()));
        return toOrderMap(saved);
    }

    @Transactional
    public void deleteOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("Order not found"));
        orderRepository.delete(order);
        createAuditEntry("order.deleted", "order", order.getId(), Map.of());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listConversations(int page, int size, Boolean blocked) {
        Pageable pageable = pageRequest(page, size, "updatedAt");
        Page<Conversation> result = Boolean.TRUE.equals(blocked)
                ? conversationRepository.findByBlockedTrueOrderByUpdatedAtDesc(pageable)
                : conversationRepository.findAllByOrderByUpdatedAtDesc(pageable);
        Page<Map<String, Object>> mapped = result.map(conversation -> {
            Map<String, Object> map = toConversationMap(conversation);
            chatMessageRepository.findTopByConversationIdAndHiddenAtIsNullOrderByCreatedAtDesc(conversation.getId())
                    .ifPresent(message -> {
                        map.put("last_message_body", message.getBody());
                        map.put("last_message_at", message.getCreatedAt());
                    });
            return map;
        });
        return pageMap(mapped, page, size);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getConversation(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
        Map<String, Object> map = toConversationMap(conversation);
        map.put("messages", chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(this::toMessageMap).toList());
        return map;
    }

    @Transactional
    public Map<String, Object> blockConversation(UUID conversationId, String reason) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
        conversation.setBlocked(true);
        conversation.setBlockedAt(Instant.now());
        conversation.setBlockedBy(currentActorId());
        conversation.setBlockedReason(reason);
        Conversation saved = conversationRepository.save(conversation);
        createAuditEntry("conversation.blocked", "conversation", saved.getId().toString(), Map.of("reason", reason));
        return toConversationMap(saved);
    }

    @Transactional
    public Map<String, Object> unblockConversation(UUID conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> ApiException.notFound("Conversation not found"));
        conversation.setBlocked(false);
        conversation.setBlockedAt(null);
        conversation.setBlockedBy(null);
        conversation.setBlockedReason(null);
        Conversation saved = conversationRepository.save(conversation);
        createAuditEntry("conversation.unblocked", "conversation", saved.getId().toString(), Map.of());
        return toConversationMap(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getChatMessage(UUID messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> ApiException.notFound("Message not found"));
        return toMessageMap(message);
    }

    @Transactional
    public Map<String, Object> hideMessage(UUID messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> ApiException.notFound("Message not found"));
        message.setHiddenAt(Instant.now());
        message.setHiddenBy(currentActorId());
        ChatMessage saved = chatMessageRepository.save(message);
        createAuditEntry("conversation.message_hidden", "chat_message", saved.getId().toString(), Map.of());
        return toMessageMap(saved);
    }

    @Transactional
    public Map<String, Object> unhideMessage(UUID messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> ApiException.notFound("Message not found"));
        message.setHiddenAt(null);
        message.setHiddenBy(null);
        ChatMessage saved = chatMessageRepository.save(message);
        createAuditEntry("conversation.message_unhidden", "chat_message", saved.getId().toString(), Map.of());
        return toMessageMap(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listReports(int page, int size, String status, UUID reportedUserId, UUID reporterId) {
        Pageable pageable = pageRequest(page, size, "createdAt");
        Page<ContentReport> reports;
        if (reportedUserId != null) {
            reports = contentReportRepository.findByReportedUserIdOrderByCreatedAtDesc(reportedUserId, pageable);
        } else if (reporterId != null) {
            reports = contentReportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId, pageable);
        } else if (status != null && !status.isBlank()) {
            reports = contentReportRepository.findByStatusOrderByCreatedAtDesc(status.trim().toLowerCase(), pageable);
        } else {
            reports = contentReportRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return pageMap(reports.map(this::toReportMap), page, size);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReport(UUID reportId) {
        ContentReport report = contentReportRepository.findById(reportId)
                .orElseThrow(() -> ApiException.notFound("Report not found"));
        return toReportMap(report);
    }

    @Transactional
    public Map<String, Object> updateReportStatus(UUID reportId, String status, String adminNotes) {
        String normalized = status == null ? "" : status.trim().toLowerCase();
        if (!REPORT_STATUSES.contains(normalized) || "pending".equals(normalized)) {
            throw ApiException.badRequest("Invalid report status");
        }
        ContentReport report = contentReportRepository.findById(reportId)
                .orElseThrow(() -> ApiException.notFound("Report not found"));
        report.setStatus(normalized);
        report.setAdminNotes(adminNotes);
        report.setReviewedAt(Instant.now());
        report.setReviewedBy(currentActorId());
        ContentReport saved = contentReportRepository.save(report);
        createAuditEntry("report.status_updated", "content_report", saved.getId().toString(),
                auditPayload("status", normalized, "admin_notes", adminNotes));
        return toReportMap(saved);
    }

    @Transactional
    public Map<String, Object> notifyReportOutcome(UUID reportId, String status, String adminNote, String title, String body) {
        ContentReport report = contentReportRepository.findById(reportId)
                .orElseThrow(() -> ApiException.notFound("Report not found"));
        UserNotification notification = UserNotification.builder()
                .userId(report.getReporterId())
                .title(title)
                .body(body)
                .iconKey("flag_outlined")
                .build();
        UserNotification saved = notificationService.saveAndPush(notification);
        createAuditEntry("report.outcome_notified", "content_report", report.getId().toString(),
                auditPayload("status", status, "admin_note", adminNote, "notification_id", saved.getId()));
        return Map.of(
                "report_id", report.getId(),
                "notification_id", saved.getId(),
                "reporter_id", report.getReporterId()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listReviews(int page, int size, String productId) {
        Pageable pageable = pageRequest(page, size, "createdAt");
        Page<ProductReview> reviews = productId != null && !productId.isBlank()
                ? productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId.trim(), pageable)
                : productReviewRepository.findAllByOrderByCreatedAtDesc(pageable);
        return pageMap(reviews.map(this::toReviewMap), page, size);
    }

    @Transactional
    public void deleteReview(UUID reviewId) {
        ProductReview review = productReviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Review not found"));
        productReviewRepository.delete(review);
        createAuditEntry("review.deleted", "product_review", review.getId().toString(), Map.of("product_id", review.getProductId()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listNotifications(int page, int size, UUID userId, Integer limit) {
        Pageable pageable = pageRequest(page, limit != null && limit > 0 ? Math.min(limit, 200) : size, "createdAt");
        Page<UserNotification> notifications = userId != null
                ? userNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : userNotificationRepository.findAllByOrderByCreatedAtDesc(pageable);
        if (limit != null && limit > 0) {
            List<Map<String, Object>> items = notifications.getContent().stream()
                    .limit(limit)
                    .map(this::toNotificationMap)
                    .toList();
            return Map.of("items", items, "total", items.size());
        }
        return pageMap(notifications.map(this::toNotificationMap), page, size);
    }

    @Transactional
    public Map<String, Object> broadcastNotification(Map<String, Object> payload) {
        String title = required(payload, "title");
        String body = required(payload, "body");
        String iconKey = payload.containsKey("icon_key") ? asString(payload.get("icon_key")) : "notifications_outlined";
        String actionUrl = payload.containsKey("action_url") ? asString(payload.get("action_url")) : null;
        UUID targetUserId = payload.containsKey("user_id") && payload.get("user_id") != null
                ? UUID.fromString(asString(payload.get("user_id")))
                : null;
        String audience = payload.containsKey("audience") ? asString(payload.get("audience")) : "all";
        UUID broadcastId = UUID.randomUUID();

        List<UserNotification> notifications = new ArrayList<>();
        if (targetUserId != null || "user".equalsIgnoreCase(audience)) {
            if (targetUserId == null) {
                throw ApiException.badRequest("user_id is required for single-user notifications");
            }
            User user = userRepository.findById(targetUserId)
                    .orElseThrow(() -> ApiException.notFound("User not found"));
            if (user.getDeletedAt() != null) {
                throw ApiException.badRequest("User account is deleted");
            }
            notifications.add(UserNotification.builder()
                    .userId(user.getId())
                    .title(title)
                    .body(body)
                    .iconKey(iconKey)
                    .actionUrl(actionUrl)
                    .broadcastId(broadcastId)
                    .build());
        } else {
            for (User user : userRepository.findAll()) {
                if (user.getDeletedAt() != null) {
                    continue;
                }
                notifications.add(UserNotification.builder()
                        .userId(user.getId())
                        .title(title)
                        .body(body)
                        .iconKey(iconKey)
                        .actionUrl(actionUrl)
                        .broadcastId(broadcastId)
                        .build());
            }
        }
        List<UserNotification> saved = userNotificationRepository.saveAll(notifications);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationService.pushSavedAll(saved);
            }
        });
        createAuditEntry("notification.broadcasted", "notification_broadcast", broadcastId.toString(),
                Map.of("title", title, "count", saved.size()));
        return Map.of(
                "broadcast_id", broadcastId,
                "sent_count", saved.size()
        );
    }

    @Transactional
    public Map<String, Object> hideNotification(UUID notificationId) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        notification.setHiddenAt(Instant.now());
        UserNotification saved = userNotificationRepository.save(notification);
        createAuditEntry("notification.hidden", "notification", saved.getId().toString(), Map.of());
        return toNotificationMap(saved);
    }

    @Transactional
    public Map<String, Object> unhideNotification(UUID notificationId) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        notification.setHiddenAt(null);
        UserNotification saved = userNotificationRepository.save(notification);
        createAuditEntry("notification.unhidden", "notification", saved.getId().toString(), Map.of());
        return toNotificationMap(saved);
    }

    @Transactional
    public Map<String, Object> markNotificationRead(UUID notificationId) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        notification.setReadAt(Instant.now());
        UserNotification saved = userNotificationRepository.save(notification);
        createAuditEntry("notification.read", "notification", saved.getId().toString(), Map.of());
        return toNotificationMap(saved);
    }

    @Transactional
    public void deleteNotification(UUID notificationId) {
        UserNotification notification = userNotificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));
        userNotificationRepository.delete(notification);
        createAuditEntry("notification.deleted", "notification", notification.getId().toString(), Map.of());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream().map(this::toCategoryMap).toList();
    }

    @Transactional
    public Map<String, Object> createCategory(Map<String, Object> payload) {
        CatalogCategory category = CatalogCategory.builder()
                .id(required(payload, "id"))
                .name(required(payload, "name"))
                .iconKey(payload.containsKey("icon_key") ? asString(payload.get("icon_key")) : "category")
                .accentHex(payload.containsKey("accent_hex") ? asString(payload.get("accent_hex")) : "C62828")
                .sortOrder(payload.containsKey("sort_order") ? asInt(payload.get("sort_order"), 0) : 0)
                .kind(payload.containsKey("kind") ? asString(payload.get("kind")) : "product")
                .build();
        CatalogCategory saved = categoryRepository.save(category);
        createAuditEntry("category.created", "catalog_category", saved.getId(), payload);
        return toCategoryMap(saved);
    }

    @Transactional
    public Map<String, Object> updateCategory(String categoryId, Map<String, Object> payload) {
        CatalogCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        if (payload.containsKey("name")) category.setName(asString(payload.get("name")));
        if (payload.containsKey("icon_key")) category.setIconKey(asString(payload.get("icon_key")));
        if (payload.containsKey("accent_hex")) category.setAccentHex(asString(payload.get("accent_hex")));
        if (payload.containsKey("sort_order")) category.setSortOrder(asInt(payload.get("sort_order"), category.getSortOrder()));
        if (payload.containsKey("kind")) category.setKind(asString(payload.get("kind")));
        CatalogCategory saved = categoryRepository.save(category);
        createAuditEntry("category.updated", "catalog_category", saved.getId(), payload);
        return toCategoryMap(saved);
    }

    @Transactional
    public void deleteCategory(String categoryId) {
        CatalogCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        categoryRepository.delete(category);
        createAuditEntry("category.deleted", "catalog_category", category.getId(), Map.of());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSubcategories(String categoryId) {
        return subcategoryRepository.findByCategoryIdOrderBySortOrderAsc(categoryId).stream()
                .map(this::toSubcategoryMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> createSubcategory(String categoryId, Map<String, Object> payload) {
        CatalogSubcategory subcategory = CatalogSubcategory.builder()
                .categoryId(categoryId)
                .id(required(payload, "id"))
                .label(required(payload, "label"))
                .sortOrder(payload.containsKey("sort_order") ? asInt(payload.get("sort_order"), 0) : 0)
                .build();
        CatalogSubcategory saved = subcategoryRepository.save(subcategory);
        createAuditEntry("subcategory.created", "catalog_subcategory", categoryId + ":" + saved.getId(), payload);
        return toSubcategoryMap(saved);
    }

    @Transactional
    public Map<String, Object> updateSubcategory(String categoryId, String subcategoryId, Map<String, Object> payload) {
        CatalogSubcategoryId id = new CatalogSubcategoryId(categoryId, subcategoryId);
        CatalogSubcategory subcategory = subcategoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Subcategory not found"));
        if (payload.containsKey("label")) subcategory.setLabel(asString(payload.get("label")));
        if (payload.containsKey("sort_order")) subcategory.setSortOrder(asInt(payload.get("sort_order"), subcategory.getSortOrder()));
        CatalogSubcategory saved = subcategoryRepository.save(subcategory);
        createAuditEntry("subcategory.updated", "catalog_subcategory", categoryId + ":" + saved.getId(), payload);
        return toSubcategoryMap(saved);
    }

    @Transactional
    public void deleteSubcategory(String categoryId, String subcategoryId) {
        CatalogSubcategoryId id = new CatalogSubcategoryId(categoryId, subcategoryId);
        CatalogSubcategory subcategory = subcategoryRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Subcategory not found"));
        subcategoryRepository.delete(subcategory);
        createAuditEntry("subcategory.deleted", "catalog_subcategory", categoryId + ":" + subcategoryId, Map.of());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMarketingBlocks() {
        return marketingBlockRepository.findAllByOrderBySortOrderAsc().stream().map(this::toMarketingBlockMap).toList();
    }

    @Transactional
    @CacheEvict(value = CacheNames.PLATFORM, allEntries = true)
    public Map<String, Object> createMarketingBlock(Map<String, Object> payload) {
        AppMarketingBlock block = AppMarketingBlock.builder()
                .id(required(payload, "id"))
                .kind(required(payload, "kind"))
                .title(required(payload, "title"))
                .subtitle(payload.containsKey("subtitle") ? asString(payload.get("subtitle")) : "")
                .gradientFrom(required(payload, "gradient_from"))
                .gradientTo(required(payload, "gradient_to"))
                .sortOrder(payload.containsKey("sort_order") ? asInt(payload.get("sort_order"), 0) : 0)
                .build();
        AppMarketingBlock saved = marketingBlockRepository.save(block);
        createAuditEntry("marketing_block.created", "app_marketing_block", saved.getId(), payload);
        return toMarketingBlockMap(saved);
    }

    @Transactional
    @CacheEvict(value = CacheNames.PLATFORM, allEntries = true)
    public Map<String, Object> updateMarketingBlock(String id, Map<String, Object> payload) {
        AppMarketingBlock block = marketingBlockRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Marketing block not found"));
        if (payload.containsKey("kind")) block.setKind(asString(payload.get("kind")));
        if (payload.containsKey("title")) block.setTitle(asString(payload.get("title")));
        if (payload.containsKey("subtitle")) block.setSubtitle(asString(payload.get("subtitle")));
        if (payload.containsKey("gradient_from")) block.setGradientFrom(asString(payload.get("gradient_from")));
        if (payload.containsKey("gradient_to")) block.setGradientTo(asString(payload.get("gradient_to")));
        if (payload.containsKey("sort_order")) block.setSortOrder(asInt(payload.get("sort_order"), block.getSortOrder()));
        AppMarketingBlock saved = marketingBlockRepository.save(block);
        createAuditEntry("marketing_block.updated", "app_marketing_block", saved.getId(), payload);
        return toMarketingBlockMap(saved);
    }

    @Transactional
    @CacheEvict(value = CacheNames.PLATFORM, allEntries = true)
    public void deleteMarketingBlock(String id) {
        AppMarketingBlock block = marketingBlockRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Marketing block not found"));
        marketingBlockRepository.delete(block);
        createAuditEntry("marketing_block.deleted", "app_marketing_block", id, Map.of());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listLegalDocuments() {
        return legalDocumentRepository.findAll(Sort.by(Sort.Direction.ASC, "slug")).stream()
                .map(this::toLegalDocumentMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLegalDocument(String slug) {
        LegalDocument document = legalDocumentRepository.findById(slug)
                .orElseThrow(() -> ApiException.notFound("Legal document not found"));
        return toLegalDocumentMap(document);
    }

    @Transactional
    @CacheEvict(value = CacheNames.PLATFORM, allEntries = true)
    public Map<String, Object> upsertLegalDocument(String slug, Map<String, Object> payload) {
        LegalDocument document = legalDocumentRepository.findById(slug).orElseGet(() -> LegalDocument.builder().slug(slug).build());
        if (payload.containsKey("title")) document.setTitle(asString(payload.get("title")));
        if (payload.containsKey("intro")) document.setIntro(asString(payload.get("intro")));
        if (payload.containsKey("sections")) document.setSections(asObjectList(payload.get("sections")));
        document.setUpdatedBy(currentActorId());
        LegalDocument saved = legalDocumentRepository.save(document);
        createAuditEntry("legal_document.upserted", "legal_document", saved.getSlug(), payload);
        return toLegalDocumentMap(saved);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSortFilters() {
        return sortFilterRepository.findAllByOrderBySortOrderAsc().stream().map(this::toSortFilterMap).toList();
    }

    @Transactional
    @CacheEvict(value = CacheNames.PLATFORM, allEntries = true)
    public Map<String, Object> createSortFilter(Map<String, Object> payload) {
        AppCategorySortFilter filter = AppCategorySortFilter.builder()
                .id(required(payload, "id"))
                .label(required(payload, "label"))
                .sortMode(payload.containsKey("sort_mode") ? asString(payload.get("sort_mode")) : "default")
                .sortOrder(payload.containsKey("sort_order") ? asInt(payload.get("sort_order"), 0) : 0)
                .build();
        AppCategorySortFilter saved = sortFilterRepository.save(filter);
        createAuditEntry("sort_filter.created", "app_category_sort_filter", saved.getId(), payload);
        return toSortFilterMap(saved);
    }

    @Transactional
    @CacheEvict(value = CacheNames.PLATFORM, allEntries = true)
    public Map<String, Object> updateSortFilter(String id, Map<String, Object> payload) {
        AppCategorySortFilter filter = sortFilterRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Sort filter not found"));
        if (payload.containsKey("label")) filter.setLabel(asString(payload.get("label")));
        if (payload.containsKey("sort_mode")) filter.setSortMode(asString(payload.get("sort_mode")));
        if (payload.containsKey("sort_order")) filter.setSortOrder(asInt(payload.get("sort_order"), filter.getSortOrder()));
        AppCategorySortFilter saved = sortFilterRepository.save(filter);
        createAuditEntry("sort_filter.updated", "app_category_sort_filter", saved.getId(), payload);
        return toSortFilterMap(saved);
    }

    @Transactional
    @CacheEvict(value = CacheNames.PLATFORM, allEntries = true)
    public void deleteSortFilter(String id) {
        AppCategorySortFilter filter = sortFilterRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Sort filter not found"));
        sortFilterRepository.delete(filter);
        createAuditEntry("sort_filter.deleted", "app_category_sort_filter", id, Map.of());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPaymentMethods() {
        return paymentMethodRepository.findAllByOrderBySortOrderAsc().stream().map(this::toPaymentMethodMap).toList();
    }

    @Transactional
    public Map<String, Object> createPaymentMethod(Map<String, Object> payload) {
        AppPaymentMethod method = AppPaymentMethod.builder()
                .id(required(payload, "id"))
                .label(required(payload, "label"))
                .iconKey(payload.containsKey("icon_key") ? asString(payload.get("icon_key")) : "payment")
                .sortOrder(payload.containsKey("sort_order") ? asInt(payload.get("sort_order"), 0) : 0)
                .isDefault(payload.containsKey("is_default") && asBoolean(payload.get("is_default")))
                .build();
        AppPaymentMethod saved = paymentMethodRepository.save(method);
        createAuditEntry("payment_method.created", "app_payment_method", saved.getId(), payload);
        return toPaymentMethodMap(saved);
    }

    @Transactional
    public Map<String, Object> updatePaymentMethod(String id, Map<String, Object> payload) {
        AppPaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Payment method not found"));
        if (payload.containsKey("label")) method.setLabel(asString(payload.get("label")));
        if (payload.containsKey("icon_key")) method.setIconKey(asString(payload.get("icon_key")));
        if (payload.containsKey("sort_order")) method.setSortOrder(asInt(payload.get("sort_order"), method.getSortOrder()));
        if (payload.containsKey("is_default")) method.setDefault(asBoolean(payload.get("is_default")));
        AppPaymentMethod saved = paymentMethodRepository.save(method);
        createAuditEntry("payment_method.updated", "app_payment_method", saved.getId(), payload);
        return toPaymentMethodMap(saved);
    }

    @Transactional
    public void deletePaymentMethod(String id) {
        AppPaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Payment method not found"));
        paymentMethodRepository.delete(method);
        createAuditEntry("payment_method.deleted", "app_payment_method", id, Map.of());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSupportSettings() {
        AppSupportSettings settings = supportSettingsRepository.findById("default")
                .orElseGet(() -> AppSupportSettings.builder().id("default").build());
        return toSupportSettingsMap(settings);
    }

    @Transactional
    @CacheEvict(value = CacheNames.PLATFORM, allEntries = true)
    public Map<String, Object> upsertSupportSettings(Map<String, Object> payload) {
        AppSupportSettings settings = supportSettingsRepository.findById("default")
                .orElseGet(() -> AppSupportSettings.builder().id("default").build());
        if (payload.containsKey("welcome_message")) settings.setWelcomeMessage(asString(payload.get("welcome_message")));
        if (payload.containsKey("quick_actions")) settings.setQuickActions(asObjectList(payload.get("quick_actions")));
        if (payload.containsKey("auto_reply_message")) settings.setAutoReplyMessage(asString(payload.get("auto_reply_message")));
        AppSupportSettings saved = supportSettingsRepository.save(settings);
        createAuditEntry("support_settings.upserted", "app_support_settings", saved.getId(), payload);
        return toSupportSettingsMap(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listAdmins(int page, int size, UUID userId) {
        if (userId != null) {
            List<Map<String, Object>> items = adminUserRepository.findByUserId(userId)
                    .map(admin -> List.of(toAdminMap(admin)))
                    .orElse(List.of());
            return Map.of("items", items, "total", items.size());
        }
        Page<AdminUser> admins = adminUserRepository.findAllByOrderByCreatedAtDesc(pageRequest(page, size, "createdAt"));
        return pageMap(admins.map(this::toAdminMap), page, size);
    }

    @Transactional
    public Map<String, Object> createAdmin(UUID userId, String role) {
        User user = userRepository.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        AdminRole adminRole = parseAdminRole(role);
        AdminUser adminUser = AdminUser.builder()
                .user(user)
                .email(user.getEmail() == null ? "" : user.getEmail())
                .role(adminRole)
                .createdBy(currentActorId())
                .build();
        AdminUser saved = adminUserRepository.save(adminUser);
        createAuditEntry("admin.created", "admin_user", saved.getUserId().toString(), Map.of("role", adminRole.name().toLowerCase()));
        return toAdminMap(saved);
    }

    @Transactional
    public Map<String, Object> updateAdminRole(UUID userId, String role) {
        AdminUser adminUser = adminUserRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Admin user not found"));
        AdminRole adminRole = parseAdminRole(role);
        adminUser.setRole(adminRole);
        AdminUser saved = adminUserRepository.save(adminUser);
        createAuditEntry("admin.role_updated", "admin_user", saved.getUserId().toString(), Map.of("role", adminRole.name().toLowerCase()));
        return toAdminMap(saved);
    }

    @Transactional
    public void deleteAdmin(UUID userId) {
        AdminUser adminUser = adminUserRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Admin user not found"));
        adminUserRepository.delete(adminUser);
        createAuditEntry("admin.deleted", "admin_user", userId.toString(), Map.of());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listAuditLogs(String q, String action, Integer limit, int page, int size) {
        int effectiveSize = limit != null && limit > 0 ? Math.min(limit, 200) : size;
        Pageable pageable = pageRequest(page, effectiveSize, "createdAt");
        String normalizedQ = normalizeQuery(q);
        String normalizedAction = action == null || action.isBlank() ? null : action.trim();
        Page<AdminAuditLog> logs = (normalizedQ == null && normalizedAction == null)
                ? auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                : auditLogRepository.search(normalizedQ, normalizedAction, pageable);
        return pageMap(logs.map(this::toAuditMap), page, effectiveSize);
    }

    @Transactional
    public Map<String, Object> createAuditEntry(String action, String entity, String entityId, Map<String, Object> payload) {
        User actor = userRepository.findById(currentActorId()).orElse(null);
        AdminAuditLog log = AdminAuditLog.builder()
                .actorId(currentActorId())
                .actorEmail(actor != null ? actor.getEmail() : null)
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .payload(payload == null ? Map.of() : payload)
                .build();
        AdminAuditLog saved = auditLogRepository.save(log);
        return toAuditMap(saved);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboardOverview() {
        long users = userRepository.count();
        long products = productRepository.count();
        long orders = orderRepository.count();
        long pendingReports = contentReportRepository.countByStatus("pending");
        return Map.of(
                "users_count", users,
                "products_count", products,
                "orders_count", orders,
                "pending_reports_count", pendingReports
        );
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.ADMIN_STATS, key = "'dashboard'")
    public Map<String, Object> adminDashboardStats() {
        Instant sevenDaysAgo = Instant.now().minusSeconds(7L * 86400);
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("users_total", userRepository.count());
        overview.put("users_last_7d", userRepository.countByCreatedAtAfter(sevenDaysAgo));
        overview.put("orders_total", orderRepository.count());
        overview.put("orders_last_7d", orderRepository.countByCreatedAtAfter(sevenDaysAgo));
        overview.put("orders_processing", orderRepository.countByStatus(OrderStatus.PROCESSING));
        overview.put("orders_shipping", orderRepository.countByStatus(OrderStatus.SHIPPING));
        overview.put("orders_delivered", orderRepository.countByStatus(OrderStatus.DELIVERED));
        overview.put("orders_cancelled", orderRepository.countByStatus(OrderStatus.CANCELLED));
        overview.put("products_total", productRepository.countActiveListings());
        overview.put("products_out_of_stock", productRepository.countActiveOutOfStock());
        overview.put("categories_total", categoryRepository.count());
        overview.put("notifications_unread", userNotificationRepository.countByReadAtIsNullAndHiddenAtIsNull());
        return Map.of("items", List.of(overview));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.ADMIN_STATS, key = "'marketplace'")
    public Map<String, Object> adminDashboardMarketplace() {
        Map<String, Object> stats = Map.of(
                "activeListings", productRepository.countActiveListings(),
                "uniqueSellers", productRepository.countDistinctActiveSellers()
        );
        return Map.of("items", List.of(stats));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.ADMIN_STATS, key = "'control'")
    public Map<String, Object> adminControlOverview() {
        long usersTotal = userRepository.count();
        long usersDeleted = userRepository.findAll().stream().filter(u -> u.getDeletedAt() != null).count();
        long usersActive = usersTotal - usersDeleted;

        Map<String, Object> control = new LinkedHashMap<>();
        control.put("users", Map.of("total", usersTotal, "active", usersActive, "deleted", usersDeleted));
        control.put("marketplace", Map.of(
                "activeListings", productRepository.countActiveListings(),
                "uniqueSellers", productRepository.countDistinctActiveSellers(),
                "totalPurchases", orderRepository.count(),
                "totalSales", orderRepository.count()
        ));
        control.put("orders", Map.of(
                "total", orderRepository.count(),
                "processing", orderRepository.countByStatus(OrderStatus.PROCESSING),
                "shipping", orderRepository.countByStatus(OrderStatus.SHIPPING),
                "delivered", orderRepository.countByStatus(OrderStatus.DELIVERED),
                "cancelled", orderRepository.countByStatus(OrderStatus.CANCELLED)
        ));
        control.put("products", Map.of(
                "active", productRepository.countActiveListings(),
                "deleted", productRepository.count() - productRepository.countActiveListings(),
                "outOfStock", productRepository.countActiveOutOfStock()
        ));
        long notificationsTotal = userNotificationRepository.count();
        long notificationsUnread = userNotificationRepository.countByReadAtIsNullAndHiddenAtIsNull();
        control.put("notifications", Map.of(
                "total", notificationsTotal,
                "unread", notificationsUnread,
                "hidden", Math.max(0, notificationsTotal - notificationsUnread)
        ));
        control.put("marketingBlocks", listMarketingBlocks());
        control.put("recentUsers", userRepository.findAll(pageRequest(0, 8, "createdAt")).getContent().stream()
                .map(this::toUserMap).toList());
        control.put("recentOrders", orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 8)).getContent().stream()
                .map(this::toOrderMap).toList());
        control.put("growth_gate", gateAlertService.getGateStatusForAdmin());
        return Map.of("items", List.of(control));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listProductsAdmin(
            String q,
            UUID sellerId,
            String categoryId,
            Boolean outOfStock,
            Boolean deletedOnly,
            int page,
            int size) {
        Pageable pageable = pageRequest(page, size, "createdAt");
        boolean onlyDeleted = Boolean.TRUE.equals(deletedOnly);
        Page<CatalogProduct> result = productRepository.adminListProducts(
                normalizeQuery(q),
                sellerId,
                categoryId,
                outOfStock,
                onlyDeleted,
                onlyDeleted,
                pageable);
        return pageMap(result.map(this::toProductMap), page, size);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listConsentsAdmin(int page, int size) {
        Page<UserConsent> consents = userConsentRepository.findAllByOrderByAcceptedAtDesc(pageRequest(page, size, "acceptedAt"));
        return pageMap(consents.map(this::toConsentMap), page, size);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAllSubcategories() {
        return subcategoryRepository.findAll(Sort.by(Sort.Order.asc("categoryId"), Sort.Order.asc("sortOrder"))).stream()
                .map(this::toSubcategoryMap)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.ADMIN_STATS, key = "'analytics:' + #period")
    public Map<String, Object> adminAnalyticsSnapshot(String period) {
        String normalized = period == null || period.isBlank() ? "day" : period.trim().toLowerCase();
        int days = switch (normalized) {
            case "week" -> 84;
            case "month" -> 365;
            case "year" -> 365 * 5;
            default -> 30;
        };
        Instant since = Instant.now().minusSeconds(days * 86400L);
        List<User> users = userRepository.findAll();
        List<CatalogProduct> products = productRepository.findAll();

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("period", normalized);
        snapshot.put("userSignups", timeSeriesFromInstant(users.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(since))
                .map(User::getCreatedAt).toList(), normalized));
        snapshot.put("userDeletions", timeSeriesFromInstant(users.stream()
                .filter(u -> u.getDeletedAt() != null && u.getDeletedAt().isAfter(since))
                .map(User::getDeletedAt).toList(), normalized));
        snapshot.put("productCreated", timeSeriesFromInstant(products.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(since))
                .map(CatalogProduct::getCreatedAt).toList(), normalized));
        snapshot.put("productDeleted", timeSeriesFromInstant(products.stream()
                .filter(p -> p.getDeletedAt() != null && p.getDeletedAt().isAfter(since))
                .map(CatalogProduct::getDeletedAt).toList(), normalized));
        snapshot.put("demographics", buildDemographicsSnapshot(users));
        snapshot.put("rpcAvailable", true);
        return Map.of("items", List.of(snapshot));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> adminAnalyticsRankings(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<CatalogProduct> activeProducts = productRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null)
                .sorted((a, b) -> Integer.compare(b.getViewCount(), a.getViewCount()))
                .limit(safeLimit)
                .toList();
        List<Map<String, Object>> topViewed = activeProducts.stream()
                .map(p -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", p.getId());
                    row.put("title", p.getTitle());
                    row.put("price_label", p.getPriceLabel());
                    row.put("view_count", p.getViewCount());
                    row.put("seller_id", p.getSellerId());
                    return row;
                }).toList();

        Map<UUID, Long> listingsBySeller = productRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null && p.getSellerId() != null)
                .collect(Collectors.groupingBy(CatalogProduct::getSellerId, Collectors.counting()));
        List<Map<String, Object>> topListedSellers = listingsBySeller.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(safeLimit)
                .map(e -> Map.<String, Object>of("seller_id", e.getKey(), "listing_count", e.getValue()))
                .toList();

        Map<String, Object> rankings = new LinkedHashMap<>();
        rankings.put("topViewed", topViewed);
        rankings.put("topListedSellers", topListedSellers);
        rankings.put("topSoldProducts", List.of());
        rankings.put("topPurchasedDeals", List.of());
        rankings.put("topSellersByOrders", List.of());
        rankings.put("topBuyers", List.of());
        return Map.of("items", List.of(rankings));
    }

    @Transactional
    public void seedLegalDocumentsDefaults() {
        upsertLegalDocument("terms", Map.of(
                "title", "Termos de Utilização",
                "intro", "Leia atentamente os termos.",
                "sections", List.of()
        ));
        upsertLegalDocument("privacy", Map.of(
                "title", "Política de Privacidade",
                "intro", "Como tratamos os seus dados.",
                "sections", List.of()
        ));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listOrdersAdmin(
            Integer limit,
            Integer timelineDays,
            String status,
            UUID userId,
            UUID sellerId,
            int page,
            int size) {
        if (timelineDays != null && timelineDays > 0) {
            Instant since = Instant.now().minusSeconds(timelineDays.longValue() * 86400);
            List<Map<String, Object>> items = orderRepository.findByCreatedAtAfterOrderByCreatedAtAsc(since).stream()
                    .map(order -> Map.<String, Object>of("created_at", order.getCreatedAt()))
                    .toList();
            return Map.of("items", items, "total", items.size());
        }

        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 200),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result;
        if (userId != null) {
            result = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        } else if (sellerId != null) {
            result = orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);
        } else {
            result = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        List<Order> orders = result.getContent();
        if (status != null && !status.isBlank()) {
            OrderStatus filter = OrderStatus.valueOf(status.trim().toUpperCase());
            orders = orders.stream().filter(o -> o.getStatus() == filter).toList();
        }
        if (limit != null && limit > 0) {
            orders = orders.stream().limit(limit).toList();
        }

        List<Map<String, Object>> items = orders.stream().map(this::toOrderMap).toList();
        return Map.of(
                "items", items,
                "page", result.getNumber(),
                "size", result.getSize(),
                "total", result.getTotalElements(),
                "total_pages", result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analyticsSignupsSeries() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Map<LocalDate, Long> counts = userRepository.findAll().stream()
                .filter(u -> u.getCreatedAt() != null)
                .collect(Collectors.groupingBy(u -> u.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(), Collectors.counting()));

        List<Map<String, Object>> series = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            series.add(Map.of(
                    "date", day.toString(),
                    "count", counts.getOrDefault(day, 0L)
            ));
        }
        return Map.of("series", series);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> analyticsDemographicsBasic() {
        List<User> users = userRepository.findAll();
        Map<String, Long> byGender = users.stream()
                .collect(Collectors.groupingBy(u -> sanitizeGroup(u.getGender()), Collectors.counting()));
        Map<String, Long> byCountry = users.stream()
                .collect(Collectors.groupingBy(u -> sanitizeGroup(u.getCountry()), Collectors.counting()));
        return Map.of(
                "by_gender", byGender,
                "by_country", byCountry
        );
    }

    @Transactional(readOnly = true)
    public String currentAdminRole() {
        return adminUserRepository.findByUserId(currentActorId())
                .map(admin -> admin.getRole().name().toLowerCase())
                .orElse("admin");
    }

    private UUID currentActorId() {
        return securityUtils.currentUserId();
    }

    /** Mapa de auditoria que aceita valores null (Map.of não aceita). */
    private static Map<String, Object> auditPayload(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("auditPayload requer pares chave/valor");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }

    private Pageable pageRequest(int page, int size, String sortField) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        return PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, sortField));
    }

    private String normalizeQuery(String q) {
        return q == null || q.isBlank() ? null : q.trim();
    }

    private Map<String, Object> pageMap(Page<Map<String, Object>> page, int pageNumber, int size) {
        return Map.of(
                "content", page.getContent(),
                "page", pageNumber,
                "size", size,
                "total_elements", page.getTotalElements(),
                "total_pages", page.getTotalPages()
        );
    }

    private Map<String, Object> toUserMap(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId());
        map.put("email", user.getEmail());
        map.put("display_name", user.getDisplayName());
        map.put("phone", user.getPhone());
        map.put("photo_url", user.getPhotoUrl());
        map.put("city", user.getCity());
        map.put("region", user.getRegion());
        map.put("country", user.getCountry());
        map.put("gender", user.getGender());
        map.put("birth_date", user.getBirthDate());
        map.put("deleted_at", user.getDeletedAt());
        map.put("banned_at", user.getBannedAt());
        map.put("banned_until", user.getBannedUntil());
        map.put("ban_reason", user.getBanReason());
        map.put("banned_by", user.getBannedBy());
        map.put("created_at", user.getCreatedAt());
        map.put("updated_at", user.getUpdatedAt());
        map.put("email_verified", user.isEmailVerified());
        map.put("phone_verified", user.isPhoneVerified());
        map.put("seller_verified", user.isSellerVerified());
        return map;
    }

    private Map<String, Object> toProductMap(CatalogProduct product) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", product.getId());
        map.put("category_id", product.getCategoryId());
        map.put("subcategory_id", product.getSubcategoryId());
        map.put("title", product.getTitle());
        map.put("price_label", product.getPriceLabel());
        map.put("old_price_label", product.getOldPriceLabel());
        map.put("delivery_text", product.getDeliveryText());
        map.put("description", product.getDescription());
        map.put("image_url", product.getImageUrl());
        map.put("image_urls", buildProductImageUrls(product));
        map.put("is_featured", product.isFeatured());
        map.put("is_out_of_stock", product.isOutOfStock());
        map.put("sort_order", product.getSortOrder());
        map.put("rating", product.getRating() != null ? product.getRating().doubleValue() : 0);
        map.put("review_count", product.getReviewCount());
        map.put("seller_id", product.getSellerId());
        map.put("deleted_at", product.getDeletedAt());
        map.put("created_at", product.getCreatedAt());
        map.put("updated_at", product.getUpdatedAt());
        return map;
    }

    private List<String> buildProductImageUrls(CatalogProduct product) {
        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            return product.getImageUrls();
        }
        if (product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
            return List.of(product.getImageUrl());
        }
        return List.of();
    }

    private Map<String, Object> toOrderMap(Order order) {
        return Map.of(
                "id", order.getId(),
                "user_id", order.getUserId(),
                "seller_id", order.getSellerId(),
                "items_count", order.getItemsCount(),
                "total_label", order.getTotalLabel(),
                "status", order.getStatus().name().toLowerCase(),
                "show_track", order.isShowTrack(),
                "created_at", order.getCreatedAt()
        );
    }

    private Map<String, Object> toConversationMap(Conversation conversation) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", conversation.getId());
        map.put("product_id", conversation.getProductId());
        map.put("buyer_id", conversation.getBuyerId());
        map.put("seller_id", conversation.getSellerId());
        map.put("is_blocked", conversation.isBlocked());
        map.put("blocked_reason", conversation.getBlockedReason());
        map.put("blocked_at", conversation.getBlockedAt());
        map.put("blocked_by", conversation.getBlockedBy());
        map.put("deal_status", conversation.getDealStatus().name().toLowerCase());
        map.put("updated_at", conversation.getUpdatedAt());
        map.put("created_at", conversation.getCreatedAt());
        return map;
    }

    private Map<String, Object> toMessageMap(ChatMessage message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", message.getId());
        map.put("conversation_id", message.getConversationId());
        map.put("sender_id", message.getSenderId());
        map.put("body", message.getBody());
        map.put("message_kind", message.getMessageKind());
        map.put("read_at", message.getReadAt());
        map.put("hidden_at", message.getHiddenAt());
        map.put("hidden_by", message.getHiddenBy());
        map.put("created_at", message.getCreatedAt());
        return map;
    }

    private Map<String, Object> toReportMap(ContentReport report) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", report.getId());
        map.put("reporter_id", report.getReporterId());
        map.put("target_type", report.getTargetType());
        map.put("target_id", report.getTargetId());
        map.put("reported_user_id", report.getReportedUserId());
        map.put("reason", report.getReason());
        map.put("details", report.getDetails());
        map.put("status", report.getStatus());
        map.put("admin_notes", report.getAdminNotes());
        map.put("reviewed_by", report.getReviewedBy());
        map.put("reviewed_at", report.getReviewedAt());
        map.put("created_at", report.getCreatedAt());
        return map;
    }

    private Map<String, Object> toReviewMap(ProductReview review) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", review.getId());
        map.put("product_id", review.getProductId());
        map.put("user_id", review.getUserId());
        map.put("rating", review.getRating());
        map.put("comment", review.getComment());
        map.put("seller_reply", review.getSellerReply());
        map.put("seller_reply_at", review.getSellerReplyAt());
        map.put("created_at", review.getCreatedAt());
        map.put("updated_at", review.getUpdatedAt());
        return map;
    }

    private Map<String, Object> toNotificationMap(UserNotification notification) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", notification.getId());
        map.put("user_id", notification.getUserId());
        map.put("title", notification.getTitle());
        map.put("body", notification.getBody());
        map.put("icon_key", notification.getIconKey());
        map.put("broadcast_id", notification.getBroadcastId());
        map.put("action_url", notification.getActionUrl());
        map.put("read_at", notification.getReadAt());
        map.put("hidden_at", notification.getHiddenAt());
        map.put("created_at", notification.getCreatedAt());
        return map;
    }

    private Map<String, Object> toCategoryMap(CatalogCategory category) {
        return Map.of(
                "id", category.getId(),
                "name", category.getName(),
                "icon_key", category.getIconKey(),
                "accent_hex", category.getAccentHex(),
                "sort_order", category.getSortOrder(),
                "kind", category.getKind()
        );
    }

    private Map<String, Object> toSubcategoryMap(CatalogSubcategory subcategory) {
        return Map.of(
                "category_id", subcategory.getCategoryId(),
                "id", subcategory.getId(),
                "label", subcategory.getLabel(),
                "sort_order", subcategory.getSortOrder()
        );
    }

    private Map<String, Object> toMarketingBlockMap(AppMarketingBlock block) {
        return Map.of(
                "id", block.getId(),
                "kind", block.getKind(),
                "title", block.getTitle(),
                "subtitle", block.getSubtitle(),
                "gradient_from", block.getGradientFrom(),
                "gradient_to", block.getGradientTo(),
                "sort_order", block.getSortOrder()
        );
    }

    private Map<String, Object> toLegalDocumentMap(LegalDocument document) {
        return Map.of(
                "slug", document.getSlug(),
                "title", document.getTitle(),
                "intro", document.getIntro(),
                "sections", document.getSections(),
                "updated_at", document.getUpdatedAt(),
                "updated_by", document.getUpdatedBy()
        );
    }

    private Map<String, Object> toSortFilterMap(AppCategorySortFilter filter) {
        return Map.of(
                "id", filter.getId(),
                "label", filter.getLabel(),
                "sort_mode", filter.getSortMode(),
                "sort_order", filter.getSortOrder()
        );
    }

    private Map<String, Object> toPaymentMethodMap(AppPaymentMethod method) {
        return Map.of(
                "id", method.getId(),
                "label", method.getLabel(),
                "icon_key", method.getIconKey(),
                "sort_order", method.getSortOrder(),
                "is_default", method.isDefault()
        );
    }

    private Map<String, Object> toSupportSettingsMap(AppSupportSettings settings) {
        return Map.of(
                "id", settings.getId(),
                "welcome_message", settings.getWelcomeMessage(),
                "quick_actions", settings.getQuickActions(),
                "auto_reply_message", settings.getAutoReplyMessage()
        );
    }

    private Map<String, Object> toAdminMap(AdminUser adminUser) {
        return Map.of(
                "user_id", adminUser.getUserId(),
                "email", adminUser.getEmail(),
                "role", adminUser.getRole().name().toLowerCase(),
                "created_at", adminUser.getCreatedAt(),
                "created_by", adminUser.getCreatedBy()
        );
    }

    private Map<String, Object> toAuditMap(AdminAuditLog log) {
        return Map.of(
                "id", log.getId(),
                "actor_id", log.getActorId(),
                "actor_email", log.getActorEmail(),
                "action", log.getAction(),
                "entity", log.getEntity(),
                "entity_id", log.getEntityId(),
                "payload", log.getPayload(),
                "created_at", log.getCreatedAt()
        );
    }

    private String sanitizeGroup(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim().toLowerCase();
    }

    private Map<String, Object> toConsentMap(UserConsent consent) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", consent.getId());
        map.put("user_id", consent.getUserId());
        map.put("consent_type", consent.getConsentType());
        map.put("accepted_at", consent.getAcceptedAt());
        map.put("user_agent", consent.getUserAgent());
        return map;
    }

    private List<Map<String, Object>> timeSeriesFromInstant(List<Instant> instants, String period) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Instant instant : instants) {
            if (instant == null) continue;
            LocalDate day = instant.atZone(ZoneOffset.UTC).toLocalDate();
            String bucket = switch (period) {
                case "year" -> String.valueOf(day.getYear());
                case "month" -> day.getYear() + "-" + String.format("%02d", day.getMonthValue());
                case "week" -> day.with(java.time.DayOfWeek.MONDAY).toString();
                default -> day.toString();
            };
            counts.merge(bucket, 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> Map.<String, Object>of("bucket", e.getKey(), "total", e.getValue()))
                .toList();
    }

    private Map<String, Object> buildDemographicsSnapshot(List<User> users) {
        long deleted = users.stream().filter(u -> u.getDeletedAt() != null).count();
        Map<String, Long> genderCounts = users.stream()
                .collect(Collectors.groupingBy(u -> sanitizeGroup(u.getGender()), Collectors.counting()));
        Map<String, Long> cityCounts = users.stream()
                .filter(u -> u.getCity() != null && !u.getCity().isBlank())
                .collect(Collectors.groupingBy(u -> u.getCity().trim(), Collectors.counting()));
        Map<String, Long> countryCounts = users.stream()
                .collect(Collectors.groupingBy(u -> sanitizeGroup(u.getCountry()), Collectors.counting()));
        Map<String, Object> demographics = new LinkedHashMap<>();
        demographics.put("total_users", users.size());
        demographics.put("deleted_users", deleted);
        demographics.put("avg_age", null);
        demographics.put("gender", genderCounts);
        demographics.put("cities", cityCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(20)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "count", e.getValue()))
                .toList());
        demographics.put("countries", countryCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(20)
                .map(e -> Map.<String, Object>of("name", e.getKey(), "count", e.getValue()))
                .toList());
        return demographics;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asObjectList(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<Map<String, Object>> normalized = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    normalized.add((Map<String, Object>) map);
                }
            }
            return normalized;
        }
        return List.of();
    }

    private int asInt(Object value, int fallback) {
        if (value == null) return fallback;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private boolean asBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private LocalDate asLocalDate(Object value) {
        String str = asString(value);
        if (str == null || str.isBlank()) return null;
        return LocalDate.parse(str);
    }

    private String required(Map<String, Object> payload, String key) {
        if (!payload.containsKey(key) || payload.get(key) == null || String.valueOf(payload.get(key)).isBlank()) {
            throw ApiException.badRequest("Missing required field: " + key);
        }
        return asString(payload.get(key));
    }

    private void requireNonEmpty(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            throw ApiException.badRequest("Nenhum campo para actualizar");
        }
    }

    private AdminRole parseAdminRole(String role) {
        if (role == null || role.isBlank()) {
            throw ApiException.badRequest("Role is required");
        }
        try {
            return AdminRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Invalid admin role");
        }
    }
}
