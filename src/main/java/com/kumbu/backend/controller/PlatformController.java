package com.kumbu.backend.controller;

import com.kumbu.backend.domain.entity.AppCategorySortFilter;
import com.kumbu.backend.domain.entity.AppMarketingBlock;
import com.kumbu.backend.domain.entity.AppSupportSettings;
import com.kumbu.backend.domain.entity.LegalDocument;
import com.kumbu.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final AppMarketingBlockRepository marketingBlockRepository;
    private final AppSupportSettingsRepository supportSettingsRepository;
    private final AppCategorySortFilterRepository sortFilterRepository;
    private final LegalDocumentRepository legalDocumentRepository;

    @GetMapping("/marketing-blocks")
    public List<Map<String, Object>> marketingBlocks() {
        return marketingBlockRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toMarketingMap).toList();
    }

    @GetMapping("/support-settings")
    public Map<String, Object> supportSettings() {
        AppSupportSettings s = supportSettingsRepository.findById("default").orElseThrow();
        Map<String, Object> m = new HashMap<>();
        m.put("id", s.getId());
        m.put("welcome_message", s.getWelcomeMessage());
        m.put("quick_actions", s.getQuickActions());
        m.put("auto_reply_message", s.getAutoReplyMessage());
        return m;
    }

    @GetMapping("/sort-filters")
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

    @GetMapping("/legal/{slug}")
    public Map<String, Object> legalDocument(@PathVariable String slug) {
        LegalDocument doc = legalDocumentRepository.findById(slug).orElseThrow();
        Map<String, Object> m = new HashMap<>();
        m.put("slug", doc.getSlug());
        m.put("title", doc.getTitle());
        m.put("intro", doc.getIntro());
        m.put("sections", doc.getSections());
        m.put("updated_at", doc.getUpdatedAt());
        return m;
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
