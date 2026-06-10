package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.MonetizationVipContactStats;
import com.kumbu.backend.repository.ConversationRepository;
import com.kumbu.backend.repository.MonetizationVipContactStatsRepository;
import com.kumbu.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonetizationVipTrackingService {

    public static final String SERVICOS_CATEGORY = "servicos";

    private final MonetizationVipContactStatsRepository statsRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final MonetizationAdminConfigService configService;

    @Transactional
    public void recordVipContact(UUID sellerId, String categoryId) {
        if (!SERVICOS_CATEGORY.equals(categoryId)) {
            return;
        }
        userRepository.findById(sellerId).ifPresent(user -> {
            if (!user.isVipActive()) {
                return;
            }
            LocalDate month = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
            MonetizationVipContactStats stats = statsRepository
                    .findByUserIdAndCategoryIdAndPeriodMonth(sellerId, categoryId, month)
                    .orElse(MonetizationVipContactStats.builder()
                            .userId(sellerId)
                            .categoryId(categoryId)
                            .periodMonth(month)
                            .build());
            stats.setContactsCount(stats.getContactsCount() + 1);
            statsRepository.save(stats);
        });
    }

    @Transactional(readOnly = true)
    public boolean isLeadsAvailableForServicos() {
        var settings = configService.getSettings();
        LocalDate monthStart = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1);
        Instant since = monthStart.atStartOfDay().toInstant(ZoneOffset.UTC);

        long categoryChats = conversationRepository.countByCategorySince(SERVICOS_CATEGORY, since);
        long vipContacts = statsRepository.sumContactsByCategoryAndMonth(SERVICOS_CATEGORY, monthStart);

        return categoryChats >= settings.getLeadsMinServicosChats()
                || vipContacts >= settings.getLeadsMinServicosChats() / 2;
    }
}
