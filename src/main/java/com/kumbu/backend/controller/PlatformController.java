package com.kumbu.backend.controller;

import com.kumbu.backend.service.PlatformService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    @GetMapping("/marketing-blocks")
    public List<Map<String, Object>> marketingBlocks() {
        return platformService.marketingBlocks();
    }

    @GetMapping("/support-settings")
    public Map<String, Object> supportSettings() {
        return platformService.supportSettings();
    }

    @GetMapping("/sort-filters")
    public List<Map<String, Object>> sortFilters() {
        return platformService.sortFilters();
    }

    @GetMapping("/legal/{slug}")
    public Map<String, Object> legalDocument(@PathVariable String slug) {
        return platformService.legalDocument(slug);
    }
}
