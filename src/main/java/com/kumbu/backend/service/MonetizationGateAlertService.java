package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.AdminUser;
import com.kumbu.backend.domain.entity.MonetizationSettings;
import com.kumbu.backend.domain.entity.UserNotification;
import com.kumbu.backend.domain.enums.AdminRole;
import com.kumbu.backend.repository.AdminUserRepository;
import com.kumbu.backend.repository.MonetizationSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Avalia o gate de crescimento (DAU, anúncios, chats) e notifica superadmins
 * quando os mínimos são atingidos e a cobrança ainda está desactivada.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonetizationGateAlertService {

    private final MonetizationMetricsService metricsService;
    private final MonetizationAdminConfigService configService;
    private final MonetizationSettingsRepository settingsRepository;
    private final AdminUserRepository adminUserRepository;
    private final NotificationService notificationService;

    @Transactional
    public Map<String, Object> evaluateAndNotify() {
        metricsService.computeAndStoreToday();
        Map<String, Object> gate = metricsService.checkMonetizationGate();
        MonetizationSettings settings = configService.getSettings();

        boolean gateReady = Boolean.TRUE.equals(gate.get("gate_ready"));
        boolean charging = settings.isChargingEnabled();
        boolean wasReady = settings.isGateLastReady();
        boolean shouldNotify = gateReady && !charging && !wasReady;

        settings.setGateLastReady(gateReady);
        if (!gateReady) {
            settings.setGateAlertSentAt(null);
        } else if (shouldNotify) {
            notifySuperAdmins(gate);
            settings.setGateAlertSentAt(Instant.now());
            log.info("MONETIZATION GATE: métricas mínimas atingidas — superadmins notificados");
        }
        settingsRepository.save(settings);

        gate.put("alert_sent_at", settings.getGateAlertSentAt());
        gate.put("needs_superadmin_review", gateReady && !charging);
        gate.put("notification_sent", shouldNotify);
        return gate;
    }

    private void notifySuperAdmins(Map<String, Object> gate) {
        @SuppressWarnings("unchecked")
        Map<String, Object> metrics = (Map<String, Object>) gate.get("metrics");
        int dau = metrics != null ? (int) metrics.getOrDefault("dau", 0) : 0;
        int listings = metrics != null ? (int) metrics.getOrDefault("active_listings", 0) : 0;
        int chats = metrics != null ? (int) metrics.getOrDefault("chats_today", 0) : 0;

        String title = "Kumbu: pronto para analisar monetização";
        String body = String.format(
                "Métricas mínimas atingidas — DAU %d, anúncios activos %d, chats hoje %d. "
                        + "Revise no Admin → Monetização antes de activar cobrança e limites VIP.",
                dau, listings, chats);

        List<AdminUser> superAdmins = adminUserRepository.findByRole(AdminRole.SUPER_ADMIN);
        for (AdminUser admin : superAdmins) {
            notificationService.saveAndPush(UserNotification.builder()
                    .userId(admin.getUserId())
                    .title(title)
                    .body(body)
                    .iconKey("notifications_outlined")
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getGateStatusForAdmin() {
        Map<String, Object> gate = metricsService.checkMonetizationGate();
        MonetizationSettings settings = configService.getSettings();
        gate.put("alert_sent_at", settings.getGateAlertSentAt());
        gate.put("needs_superadmin_review", Boolean.TRUE.equals(gate.get("gate_ready")) && !settings.isChargingEnabled());
        return gate;
    }
}
