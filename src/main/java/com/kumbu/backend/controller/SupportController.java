package com.kumbu.backend.controller;

import com.kumbu.backend.dto.chat.ChatMessageResponse;
import com.kumbu.backend.dto.support.SupportConversationResponse;
import com.kumbu.backend.dto.support.SupportMessageRequest;
import com.kumbu.backend.dto.support.SupportQuickActionRequest;
import com.kumbu.backend.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @GetMapping("/conversation")
    public SupportConversationResponse conversation() {
        return supportService.getOrOpenConversation();
    }

    @GetMapping("/conversation/messages")
    public List<ChatMessageResponse> messages() {
        return supportService.listMessages();
    }

    @PostMapping("/conversation/messages")
    public List<ChatMessageResponse> sendMessage(@Valid @RequestBody SupportMessageRequest request) {
        return supportService.sendUserMessage(request.getBody(), request.getAttachmentUrl());
    }

    @PostMapping("/conversation/quick-action")
    public List<ChatMessageResponse> quickAction(@Valid @RequestBody SupportQuickActionRequest request) {
        return supportService.selectQuickAction(request.getActionId());
    }
}
