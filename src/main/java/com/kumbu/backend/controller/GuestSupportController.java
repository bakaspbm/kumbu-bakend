package com.kumbu.backend.controller;

import com.kumbu.backend.dto.chat.ChatMessageResponse;
import com.kumbu.backend.dto.support.GuestSupportOpenRequest;
import com.kumbu.backend.dto.support.GuestSupportSessionResponse;
import com.kumbu.backend.dto.support.SupportMessageRequest;
import com.kumbu.backend.dto.support.SupportQuickActionRequest;
import com.kumbu.backend.service.SupportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/support/guest")
@RequiredArgsConstructor
public class GuestSupportController {

    private final SupportService supportService;

    @PostMapping("/session")
    public GuestSupportSessionResponse openSession(@Valid @RequestBody GuestSupportOpenRequest request) {
        return supportService.openGuestSession(request.getName(), request.getEmail());
    }

    @GetMapping("/session")
    public GuestSupportSessionResponse getSession(
            @RequestHeader("X-Guest-Support-Token") String token) {
        return supportService.getGuestSession(token);
    }

    @GetMapping("/messages")
    public List<ChatMessageResponse> messages(
            @RequestHeader("X-Guest-Support-Token") String token) {
        return supportService.listGuestMessages(token);
    }

    @PostMapping("/messages")
    public List<ChatMessageResponse> sendMessage(
            @RequestHeader("X-Guest-Support-Token") String token,
            @Valid @RequestBody SupportMessageRequest request) {
        return supportService.sendGuestMessage(token, request.getBody(), request.getAttachmentUrl());
    }

    @PostMapping("/quick-action")
    public List<ChatMessageResponse> quickAction(
            @RequestHeader("X-Guest-Support-Token") String token,
            @Valid @RequestBody SupportQuickActionRequest request) {
        return supportService.selectGuestQuickAction(token, request.getActionId());
    }
}
