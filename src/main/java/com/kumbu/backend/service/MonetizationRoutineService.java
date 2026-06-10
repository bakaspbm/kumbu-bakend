package com.kumbu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MonetizationRoutineService {

    private final MonetizationPhaseService phaseService;
    private final MonetizationMetricsService metricsService;
    private final MonetizationAnalyticsService analyticsService;
    private final MonetizationAdminConfigService configService;
    private final MonetizationCategoryService categoryService;
    private final MonetizationPaymentService paymentService;

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminOverviewWithRoutine() {
        metricsService.computeAndStoreToday();

        Map<String, Object> result = new LinkedHashMap<>(phaseService.getAdminOverview());
        var settings = configService.getSettings();
        Map<String, Object> gate = metricsService.checkMonetizationGate();
        List<Map<String, Object>> overdue = analyticsService.getOverduePayments();

        result.put("settings", configService.toSettingsMap(settings));
        result.put("category_matrix", categoryService.getMonetizationMatrix());
        result.put("key_metrics", analyticsService.getKeyMetrics());
        result.put("routine", buildRoutine(gate, overdue));
        result.put("overdue_payments", overdue);
        result.put("overdue_payments_count", overdue.size());
        result.put("recommended_provider", paymentService.getRecommendedProvider());
        return result;
    }

    private Map<String, Object> buildRoutine(Map<String, Object> gate, List<Map<String, Object>> overdue) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        boolean isMonday = today.getDayOfWeek() == DayOfWeek.MONDAY;
        boolean isFirstOfMonth = today.getDayOfMonth() == 1;
        boolean gateReady = Boolean.TRUE.equals(gate.get("gate_ready"));
        boolean chargingEnabled = configService.getSettings().isChargingEnabled();

        Map<String, Object> routine = new LinkedHashMap<>();

        // Rotina semanal (segunda)
        routine.put("weekly", Map.of(
                "day", "MONDAY",
                "is_today", isMonday,
                "tasks", List.of(
                        task("review_metrics", "Rever métricas + gate", isMonday, gateReady
                                ? "Gate OK — considerar activar cobrança"
                                : "Gate ainda não atingido — não cobrar"),
                        task("review_phases", "Rever fases activas", isMonday, null),
                        task("review_category_matrix", "Rever matriz por categoria", isMonday, null)
                )
        ));

        // Rotina diária
        boolean dailyUrgent = !overdue.isEmpty();
        routine.put("daily", Map.of(
                "tasks", List.of(
                        task("confirm_payments", "Confirmar pagamentos pendentes (SLA 24h)",
                                true,
                                overdue.isEmpty()
                                        ? "Nenhum pagamento em atraso ✓"
                                        : overdue.size() + " pagamento(s) há mais de 24h — URGENTE"),
                        task("check_proof_submissions", "Verificar comprovativos Multicaixa", true, null)
                ),
                "urgent", dailyUrgent,
                "urgent_count", overdue.size()
        ));

        // Rotina mensal
        routine.put("monthly", Map.of(
                "day", "DAY_1",
                "is_today", isFirstOfMonth,
                "tasks", List.of(
                        task("price_review", "Rever preços por categoria (conversão)", isFirstOfMonth,
                                "Ver GET /admin/monetization/analytics/price-review"),
                        task("servicos_vip_review", "Avaliar VIP serviços vs leads", isFirstOfMonth, null)
                ),
                "price_review_available", true
        ));

        // Estado global
        routine.put("charging_status", Map.of(
                "gate_ready", gateReady,
                "charging_enabled", chargingEnabled,
                "can_charge", gateReady && chargingEnabled,
                "recommendation", !gateReady
                        ? "NÃO COBRAR — gatilhos não atingidos"
                        : !chargingEnabled
                        ? "Gate OK — activar cobrança manualmente quando estiver pronto"
                        : "Cobrança activa"
        ));

        routine.put("payment_policy", Map.of(
                "primary_method", "MULTICAIXA_EXPRESS",
                "message", "Multicaixa Express primeiro. Transferências bancárias só quando activadas no admin.",
                "bank_transfers_enabled", configService.getSettings().isBankTransfersEnabled()
        ));

        return routine;
    }

    private Map<String, Object> task(String id, String label, boolean relevantToday, String note) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("id", id);
        t.put("label", label);
        t.put("relevant_today", relevantToday);
        if (note != null) t.put("note", note);
        return t;
    }

    @Transactional
    public Map<String, Object> enableCharging() {
        Map<String, Object> gate = metricsService.checkMonetizationGate();
        if (!Boolean.TRUE.equals(gate.get("gate_ready"))) {
            throw com.kumbu.backend.exception.ApiException.badRequest(
                    "Não pode activar cobrança: gatilhos não atingidos. " + gate.get("message"));
        }
        return configService.setChargingEnabled(true);
    }

    @Transactional
    public Map<String, Object> disableCharging() {
        return configService.setChargingEnabled(false);
    }
}
