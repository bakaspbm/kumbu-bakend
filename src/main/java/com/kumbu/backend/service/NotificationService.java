package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.UserNotification;
import com.kumbu.backend.dto.notification.NotificationPushEvent;
import com.kumbu.backend.repository.UserNotificationRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private static final String USER_NOTIFICATIONS_DEST = "/queue/notifications";

    private final UserNotificationRepository notificationRepository;
    private final SecurityUtils securityUtils;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional(readOnly = true)
    public List<NotificationDto> listMine() {
        UUID userId = securityUtils.currentUserId();
        return notificationRepository.findByUserIdAndHiddenAtIsNullOrderByCreatedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long countUnread() {
        return notificationRepository.countByUserIdAndReadAtIsNullAndHiddenAtIsNull(securityUtils.currentUserId());
    }

    @Transactional(readOnly = true)
    public long countUnreadForUser(UUID userId) {
        return notificationRepository.countByUserIdAndReadAtIsNullAndHiddenAtIsNull(userId);
    }

    @Transactional
    public void markRead(UUID notificationId) {
        UUID userId = securityUtils.currentUserId();
        UserNotification n = notificationRepository.findById(notificationId)
                .filter(x -> userId.equals(x.getUserId()))
                .orElseThrow();
        n.setReadAt(Instant.now());
        notificationRepository.save(n);
        pushSync(userId);
    }

    @Transactional
    public UserNotification saveAndPush(UserNotification notification) {
        UserNotification saved = notificationRepository.save(notification);
        schedulePush(saved);
        return saved;
    }

    @Transactional
    public void pushSavedAll(Collection<UserNotification> notifications) {
        for (UserNotification notification : notifications) {
            pushNew(notification);
        }
    }

    public void pushNew(UserNotification notification) {
        if (notification == null || notification.getUserId() == null) return;
        UUID userId = notification.getUserId();
        long unread = notificationRepository.countByUserIdAndReadAtIsNullAndHiddenAtIsNull(userId);
        push(userId, NotificationPushEvent.builder()
                .type("new")
                .notification(toDto(notification))
                .unreadCount(unread)
                .build());
    }

    public void pushSync(UUID userId) {
        if (userId == null) return;
        push(userId, NotificationPushEvent.builder()
                .type("sync")
                .unreadCount(notificationRepository.countByUserIdAndReadAtIsNullAndHiddenAtIsNull(userId))
                .build());
    }

    private void push(UUID userId, NotificationPushEvent event) {
        try {
            messagingTemplate.convertAndSendToUser(userId.toString(), USER_NOTIFICATIONS_DEST, event);
        } catch (Exception ex) {
            log.debug("Falha ao enviar notificação em tempo real para {}: {}", userId, ex.getMessage());
        }
    }

    private void schedulePush(UserNotification saved) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            pushNew(saved);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pushNew(saved);
            }
        });
    }

    private NotificationDto toDto(UserNotification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .iconKey(n.getIconKey())
                .createdAt(n.getCreatedAt())
                .readAt(n.getReadAt())
                .actionUrl(n.getActionUrl())
                .build();
    }

    @Data
    @Builder
    public static class NotificationDto {
        private UUID id;
        private String title;
        private String body;
        private String iconKey;
        private Instant createdAt;
        private Instant readAt;
        private String actionUrl;
    }
}
