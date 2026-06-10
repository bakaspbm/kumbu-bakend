package com.kumbu.backend.controller;



import com.kumbu.backend.dto.admin.AdminBlockConversationRequest;

import com.kumbu.backend.service.AdminManagementService;

import jakarta.validation.Valid;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.util.Map;

import java.util.UUID;



@RestController

@RequestMapping("/api/v1/admin/conversations")

@RequiredArgsConstructor

@Validated

public class AdminConversationsController {



    private final AdminManagementService adminManagementService;



    @GetMapping

    public Map<String, Object> list(

            @RequestParam(required = false) Boolean blocked,

            @RequestParam(defaultValue = "0") @Min(0) int page,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return adminManagementService.listConversations(page, size, blocked);

    }



    @GetMapping("/messages/{messageId}")

    public Map<String, Object> getMessage(@PathVariable UUID messageId) {

        return adminManagementService.getChatMessage(messageId);

    }



    @GetMapping("/{id}")

    public Map<String, Object> get(@PathVariable UUID id) {

        return adminManagementService.getConversation(id);

    }



    @PostMapping("/{id}/block")

    public Map<String, Object> block(

            @PathVariable UUID id,

            @RequestBody(required = false) @Valid AdminBlockConversationRequest request) {

        String reason = request != null ? request.getReason() : null;

        return adminManagementService.blockConversation(id, reason);

    }



    @PostMapping("/{id}/unblock")

    public Map<String, Object> unblock(@PathVariable UUID id) {

        return adminManagementService.unblockConversation(id);

    }



    @PostMapping("/messages/{messageId}/hide")

    public Map<String, Object> hideMessage(@PathVariable UUID messageId) {

        return adminManagementService.hideMessage(messageId);

    }



    @PostMapping("/messages/{messageId}/unhide")

    public Map<String, Object> unhideMessage(@PathVariable UUID messageId) {

        return adminManagementService.unhideMessage(messageId);

    }

}

