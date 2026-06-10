package com.kumbu.backend.controller;

import com.kumbu.backend.dto.chat.ChatMessageResponse;
import com.kumbu.backend.dto.support.SupportMessageRequest;
import com.kumbu.backend.service.SupportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/support")
@RequiredArgsConstructor
@Validated
public class AdminSupportController {

    private final SupportService supportService;

    @GetMapping("/conversations/waiting-count")
    public Map<String, Object> waitingCount() {
        return Map.of("count", supportService.countWaitingAdmin());
    }

    @GetMapping("/conversations")
    public Map<String, Object> listConversations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return supportService.listAdminConversations(status, page, size);
    }

    @GetMapping("/conversations/{id}")
    public Map<String, Object> getConversation(@PathVariable UUID id) {
        return supportService.getAdminConversation(id);
    }

    @PostMapping("/conversations/{id}/messages")
    public ChatMessageResponse reply(
            @PathVariable UUID id,
            @Valid @RequestBody SupportMessageRequest request) {
        return supportService.adminReply(id, request.getBody());
    }

    @PostMapping("/conversations/{id}/close")
    public Map<String, Object> close(@PathVariable UUID id) {
        return supportService.adminCloseConversation(id);
    }
}
