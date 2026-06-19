package com.kumbu.backend.service;

import com.kumbu.backend.config.CacheNames;
import com.kumbu.backend.domain.entity.MonetizationDailyMetrics;
import com.kumbu.backend.repository.CatalogProductRepository;
import com.kumbu.backend.repository.ChatMessageRepository;
import com.kumbu.backend.repository.MonetizationDailyMetricsRepository;
import com.kumbu.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MonetizationMetricsService {

    private final UserRepository userRepository;
    private final CatalogProductRepository productRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MonetizationDailyMetricsRepository metricsRepository;
    private final MonetizationAdminConfigService configService;

    @Transactional
    @CacheEvict(value = CacheNames.MONETIZATION, allEntries = true)
    public Map<String, Object> computeAndStoreToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant startOfDay = today.atStartOfDay().toInstant(ZoneOffset.UTC);

        int totalUsers = (int) userRepository.count();
        int activeListings = (int) productRepository.countActiveListings();
        int chatsToday = (int) chatMessageRepository.countSince(startOfDay);
        int dau = (int) chatMessageRepository.countDistinctSendersSince(startOfDay);

        MonetizationDailyMetrics metrics = metricsRepository.findById(today)
                .orElse(MonetizationDailyMetrics.builder().metricDate(today).build());
        metrics.setTotalUsers(totalUsers);
        metrics.setActiveListings(activeListings);
        metrics.setChatsToday(chatsToday);
        metrics.setDau(dau);
        metrics.setComputedAt(Instant.now());
        metricsRepository.save(metrics);

        return toMap(metrics);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.MONETIZATION, key = "'metrics:current'")
    public Map<String, Object> getCurrentMetrics() {
        return metricsRepository.findFirstByOrderByMetricDateDesc()
                .map(this::toMap)
                .orElseGet(this::computeAndStoreToday);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.MONETIZATION, key = "'gate'")
    public Map<String, Object> checkMonetizationGate() {
        var settings = configService.getSettings();
        Map<String, Object> metrics = getCurrentMetrics();
        int dau = (int) metrics.get("dau");
        int listings = (int) metrics.get("active_listings");
        int chats = (int) metrics.get("chats_today");

        boolean dauOk = dau >= settings.getGateMinDau();
        boolean listingsOk = listings >= settings.getGateMinListings();
        boolean chatsOk = chats >= settings.getGateMinChats();
        boolean gateReady = dauOk && listingsOk && chatsOk;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("gate_ready", gateReady);
        result.put("metrics", metrics);
        result.put("requirements", Map.of(
                "min_dau", settings.getGateMinDau(),
                "max_dau", settings.getGateMaxDau(),
                "min_listings", settings.getGateMinListings(),
                "max_listings", settings.getGateMaxListings(),
                "min_chats_per_day", settings.getGateMinChats(),
                "max_chats_per_day", settings.getGateMaxChats()
        ));
        result.put("checks", Map.of(
                "dau_ok", dauOk,
                "dau_current", dau,
                "dau_progress_pct", progressPct(dau, settings.getGateMinDau(), settings.getGateMaxDau()),
                "listings_ok", listingsOk,
                "listings_current", listings,
                "listings_progress_pct", progressPct(listings, settings.getGateMinListings(), settings.getGateMaxListings()),
                "chats_ok", chatsOk,
                "chats_current", chats,
                "chats_progress_pct", progressPct(chats, settings.getGateMinChats(), settings.getGateMaxChats())
        ));
        result.put("message", gateReady
                ? "Métricas mínimas atingidas! Analise no admin antes de activar cobrança."
                : "Ainda não atingiu os gatilhos mínimos para começar a cobrar.");
        result.put("charging_enabled", settings.isChargingEnabled());
        result.put("can_charge", gateReady && settings.isChargingEnabled());
        result.put("listing_limits_enforced", settings.isChargingEnabled());
        result.put("policy", settings.isChargingEnabled()
                ? "Limites de anúncios activos — planos VIP aplicam-se."
                : "Sem limites de anúncios — publicação gratuita até activar cobrança no admin.");
        result.put("recommendation", !gateReady
                ? "NÃO COBRAR — crescer volume primeiro"
                : !settings.isChargingEnabled()
                ? "Gate OK — superadmin deve analisar e activar cobrança manualmente"
                : "Cobrança activa");
        return result;
    }

    private static int progressPct(int current, int min, int max) {
        if (current >= min) return 100;
        if (min <= 0) return 0;
        return Math.min(99, (int) Math.round((current * 100.0) / min));
    }

    private Map<String, Object> toMap(MonetizationDailyMetrics m) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("metric_date", m.getMetricDate().toString());
        map.put("dau", m.getDau());
        map.put("total_users", m.getTotalUsers());
        map.put("active_listings", m.getActiveListings());
        map.put("chats_today", m.getChatsToday());
        map.put("computed_at", m.getComputedAt());
        return map;
    }
}
