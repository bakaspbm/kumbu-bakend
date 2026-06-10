package com.kumbu.backend.dto.notification;

import com.kumbu.backend.service.NotificationService;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationPushEvent {
    /** "new" — nova notificação; "sync" — actualizar contador. */
    private String type;
    private NotificationService.NotificationDto notification;
    private Long unreadCount;
}
