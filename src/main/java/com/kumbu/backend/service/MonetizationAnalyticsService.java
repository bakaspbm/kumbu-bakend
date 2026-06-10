package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.MonetizationPayment;
import com.kumbu.backend.domain.entity.MonetizationProduct;
import com.kumbu.backend.domain.enums.MonetizationFeatureType;
import com.kumbu.backend.domain.enums.PlatformPaymentStatus;
import com.kumbu.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MonetizationAnalyticsService {

    private final MonetizationPaymentRepository paymentRepository;
    private final MonetizationProductRepository productRepository;
    private final ListingPromotionRepository listingPromotionRepository;
    private final ConversationRepository conversationRepository;
    private final MonetizationVipContactStatsRepository vipStatsRepository;
    private final MonetizationAdminConfigService configService;

    @Transactional(readOnly = true)
    public Map<String, Object> getKeyMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        long confirmed = paymentRepository.countConfirmed();
        long withProofResolved = paymentRepository.countWithProofResolved();
        double confirmationRate = withProofResolved > 0
                ? Math.round(1000.0 * confirmed / withProofResolved) / 10.0
                : 0.0;

        metrics.put("payment_confirmation_rate_pct", confirmationRate);
        metrics.put("payments_confirmed", confirmed);
        metrics.put("payments_awaiting", paymentRepository.countAwaitingConfirmation());
        metrics.put("revenue_by_category", getRevenueByCategory());
        metrics.put("promotion_conversion", getPromotionConversion());
        metrics.put("servicos_vip_tracking", getServicosVipMetrics());
        metrics.put("avg_payment_confirm_hours", estimateAvgConfirmHours());

        return metrics;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getOverduePayments() {
        int slaHours = configService.getSettings().getPaymentSlaHours();
        Instant deadline = Instant.now().minus(slaHours, ChronoUnit.HOURS);

        return paymentRepository.findOverdueForSla(deadline).stream()
                .map(p -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", p.getId());
                    map.put("reference_code", p.getReferenceCode());
                    map.put("amount_kz", p.getAmountKz());
                    map.put("amount_label", MonetizationPhaseService.formatKz(p.getAmountKz()));
                    map.put("status", p.getStatus().name());
                    map.put("user_id", p.getUserId());
                    map.put("product_id", p.getProductId());
                    map.put("created_at", p.getCreatedAt());
                    map.put("hours_waiting", ChronoUnit.HOURS.between(p.getCreatedAt(), Instant.now()));
                    map.put("has_proof", p.getProofUrl() != null);
                    map.put("urgency", p.getProofUrl() != null ? "HIGH" : "MEDIUM");
                    return map;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMonthlyPriceReview() {
        LocalDate monthStart = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        Instant since = monthStart.atStartOfDay().toInstant(ZoneOffset.UTC);

        Map<String, List<MonetizationPayment>> paymentsByProduct = paymentRepository.findAll().stream()
                .filter(p -> p.getCreatedAt().isAfter(since))
                .collect(Collectors.groupingBy(MonetizationPayment::getProductId));

        List<Map<String, Object>> suggestions = new ArrayList<>();

        for (MonetizationProduct product : productRepository.findAll()) {
            List<MonetizationPayment> payments = paymentsByProduct.getOrDefault(product.getId(), List.of());
            long initiated = payments.size();
            long confirmed = payments.stream().filter(p -> p.getStatus() == PlatformPaymentStatus.CONFIRMED).count();
            double rate = initiated > 0 ? (double) confirmed / initiated : 0;

            String suggestion = "MANTER";
            String reason = "Dados insuficientes este mês";

            if (initiated >= 5) {
                if (rate >= 0.7) {
                    suggestion = "AUMENTAR";
                    reason = String.format("Alta conversão (%.0f%%) — pode testar +10-20%%", rate * 100);
                } else if (rate < 0.3) {
                    suggestion = "REDUZIR";
                    reason = String.format("Baixa conversão (%.0f%%) — testar preço mais baixo", rate * 100);
                } else {
                    suggestion = "MANTER";
                    reason = String.format("Conversão moderada (%.0f%%)", rate * 100);
                }
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("product_id", product.getId());
            row.put("product_name", product.getName());
            row.put("category_id", product.getCategoryId());
            row.put("feature_type", product.getFeatureType().name());
            row.put("current_price_kz", product.getPriceKz());
            row.put("current_price_label", MonetizationPhaseService.formatKz(product.getPriceKz()));
            row.put("payments_initiated", initiated);
            row.put("payments_confirmed", confirmed);
            row.put("conversion_rate_pct", Math.round(rate * 1000) / 10.0);
            row.put("suggestion", suggestion);
            row.put("reason", reason);
            if ("AUMENTAR".equals(suggestion)) {
                row.put("suggested_price_kz", Math.round(product.getPriceKz() * 1.15));
            } else if ("REDUZIR".equals(suggestion)) {
                row.put("suggested_price_kz", Math.round(product.getPriceKz() * 0.85));
            }
            suggestions.add(row);
        }

        suggestions.sort((a, b) -> Long.compare(
                (long) b.get("payments_initiated"),
                (long) a.get("payments_initiated")));

        return suggestions;
    }

    private Map<String, Object> getRevenueByCategory() {
        Map<String, Long> revenue = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();

        paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PlatformPaymentStatus.CONFIRMED)
                .forEach(p -> {
                    productRepository.findById(p.getProductId()).ifPresent(prod -> {
                        String cat = prod.getCategoryId() != null ? prod.getCategoryId() : "geral";
                        revenue.merge(cat, p.getAmountKz(), Long::sum);
                        counts.merge(cat, 1L, Long::sum);
                    });
                });

        List<Map<String, Object>> rows = revenue.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> Map.<String, Object>of(
                        "category_id", e.getKey(),
                        "revenue_kz", e.getValue(),
                        "revenue_label", MonetizationPhaseService.formatKz(e.getValue()),
                        "payments_count", counts.getOrDefault(e.getKey(), 0L)
                ))
                .toList();

        return Map.of("categories", rows, "total_kz", revenue.values().stream().mapToLong(Long::longValue).sum());
    }

    private Map<String, Object> getPromotionConversion() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        var promotions = listingPromotionRepository.findAll().stream()
                .filter(p -> p.getCreatedAt().isAfter(thirtyDaysAgo))
                .toList();

        long converted = promotions.stream()
                .filter(p -> conversationRepository.existsByProductIdAndCreatedAtGreaterThanEqual(
                        p.getProductId(), p.getStartsAt()))
                .count();

        double rate = promotions.isEmpty() ? 0 : Math.round(1000.0 * converted / promotions.size()) / 10.0;

        return Map.of(
                "promotions_last_30d", promotions.size(),
                "promotions_with_contacts", converted,
                "conversion_rate_pct", rate,
                "note", "Anúncios promovidos que receberam pelo menos 1 contacto"
        );
    }

    private Map<String, Object> getServicosVipMetrics() {
        LocalDate month = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        Instant since = month.atStartOfDay().toInstant(ZoneOffset.UTC);
        long categoryChats = conversationRepository.countByCategorySince("servicos", since);
        long vipContacts = vipStatsRepository.sumContactsByCategoryAndMonth("servicos", month);
        var settings = configService.getSettings();

        return Map.of(
                "category_chats_this_month", categoryChats,
                "vip_contacts_this_month", vipContacts,
                "leads_threshold", settings.getLeadsMinServicosChats(),
                "leads_unlocked", categoryChats >= settings.getLeadsMinServicosChats(),
                "message", categoryChats >= settings.getLeadsMinServicosChats()
                        ? "Volume suficiente — pode activar leads pagos"
                        : "VIP primeiro — leads pagos bloqueados até atingir volume"
        );
    }

    private double estimateAvgConfirmHours() {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getStatus() == PlatformPaymentStatus.CONFIRMED)
                .filter(p -> p.getConfirmedAt() != null)
                .mapToLong(p -> ChronoUnit.HOURS.between(p.getCreatedAt(), p.getConfirmedAt()))
                .average()
                .orElse(0);
    }
}
