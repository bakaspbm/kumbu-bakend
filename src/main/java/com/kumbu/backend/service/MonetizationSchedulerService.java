package com.kumbu.backend.service;

import com.kumbu.backend.repository.CatalogProductRepository;
import com.kumbu.backend.repository.ListingPromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonetizationSchedulerService {

    private final CatalogProductRepository catalogProductRepository;
    private final ListingPromotionRepository listingPromotionRepository;
    private final MonetizationAnalyticsService analyticsService;
    private final MonetizationGateAlertService gateAlertService;

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void dailyMetricsJob() {
        log.info("Computing daily monetization metrics");
        gateAlertService.evaluateAndNotify();
    }

    /** Verifica gate de crescimento de hora a hora (notifica superadmin na transição). */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void hourlyGateCheckJob() {
        gateAlertService.evaluateAndNotify();
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void dailyOverduePaymentAlertJob() {
        var overdue = analyticsService.getOverduePayments();
        if (!overdue.isEmpty()) {
            log.warn("MONETIZATION SLA: {} pagamento(s) pendente(s) há mais de 24h — confirmar no admin",
                    overdue.size());
        }
    }

    @Scheduled(cron = "0 0 7 * * MON")
    @Transactional
    public void weeklyMondayReviewJob() {
        log.info("Weekly monetization review — ver GET /admin/monetization/overview");
        gateAlertService.evaluateAndNotify();
    }

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expirePromotionsJob() {
        Instant now = Instant.now();
        listingPromotionRepository.findAllActive(now).stream()
                .filter(p -> p.getEndsAt() != null && p.getEndsAt().isBefore(now))
                .forEach(p -> {
                    p.setActive(false);
                    listingPromotionRepository.save(p);
                });

        catalogProductRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null)
                .filter(p -> p.getFeaturedUntil() != null && p.getFeaturedUntil().isBefore(now))
                .forEach(p -> {
                    p.setFeatured(false);
                    p.setHighlightType(null);
                    p.setFeaturedUntil(null);
                    catalogProductRepository.save(p);
                });

        catalogProductRepository.findAll().stream()
                .filter(p -> p.getDeletedAt() == null)
                .filter(p -> p.getBoostedUntil() != null && p.getBoostedUntil().isBefore(now))
                .forEach(p -> {
                    p.setBoostScore(0);
                    p.setBoostedUntil(null);
                    catalogProductRepository.save(p);
                });
    }
}
