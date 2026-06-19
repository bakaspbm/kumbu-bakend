package com.kumbu.backend.service;

import com.kumbu.backend.config.CacheNames;
import com.kumbu.backend.domain.entity.AppCategorySortFilter;
import com.kumbu.backend.domain.entity.AppMarketingBlock;
import com.kumbu.backend.domain.entity.AppSupportSettings;
import com.kumbu.backend.domain.entity.LegalDocument;
import com.kumbu.backend.repository.AppCategorySortFilterRepository;
import com.kumbu.backend.repository.AppMarketingBlockRepository;
import com.kumbu.backend.repository.AppSupportSettingsRepository;
import com.kumbu.backend.repository.LegalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlatformService {

    private final AppMarketingBlockRepository marketingBlockRepository;
    private final AppSupportSettingsRepository supportSettingsRepository;
    private final AppCategorySortFilterRepository sortFilterRepository;
    private final LegalDocumentRepository legalDocumentRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PLATFORM, key = "'marketing-blocks'")
    public List<Map<String, Object>> marketingBlocks() {
        return marketingBlockRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toMarketingMap)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PLATFORM, key = "'support-settings'")
    public Map<String, Object> supportSettings() {
        AppSupportSettings s = supportSettingsRepository.findById("default").orElseThrow();
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("welcome_message", s.getWelcomeMessage());
        m.put("quick_actions", s.getQuickActions());
        m.put("auto_reply_message", s.getAutoReplyMessage());
        return m;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PLATFORM, key = "'sort-filters'")
    public List<Map<String, Object>> sortFilters() {
        return sortFilterRepository.findAllByOrderBySortOrderAsc().stream()
                .map(f -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", f.getId());
                    m.put("label", f.getLabel());
                    m.put("sort_mode", f.getSortMode());
                    m.put("sort_order", f.getSortOrder());
                    return m;
                }).toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PLATFORM, key = "'legal:' + #slug")
    public Map<String, Object> legalDocument(String slug) {
        LegalDocument doc = legalDocumentRepository.findById(slug).orElseThrow();
        Map<String, Object> m = new HashMap<>();
        m.put("slug", doc.getSlug());
        m.put("title", doc.getTitle());
        m.put("intro", doc.getIntro());
        m.put("sections", doc.getSections());
        m.put("updated_at", doc.getUpdatedAt());
        return m;
    }

    @CacheEvict(value = CacheNames.PLATFORM, allEntries = true)
    public void evictAll() {
        // invalidação via anotação
    }

    private Map<String, Object> toMarketingMap(AppMarketingBlock b) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", b.getId());
        m.put("kind", b.getKind());
        m.put("title", b.getTitle());
        m.put("subtitle", b.getSubtitle());
        m.put("gradient_from", b.getGradientFrom());
        m.put("gradient_to", b.getGradientTo());
        m.put("sort_order", b.getSortOrder());
        return m;
    }
}
