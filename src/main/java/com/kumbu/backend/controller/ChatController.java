package com.kumbu.backend.controller;



import com.kumbu.backend.dto.chat.ChatMessageResponse;

import com.kumbu.backend.dto.chat.ConversationResponse;

import com.kumbu.backend.dto.chat.DealStatusRequest;

import com.kumbu.backend.dto.chat.SendMessageRequest;

import com.kumbu.backend.dto.chat.StartConversationRequest;

import com.kumbu.backend.service.ChatService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;



import java.util.List;

import java.util.UUID;



@RestController

@RequestMapping("/api/v1/chat")

@RequiredArgsConstructor

public class ChatController {



    private final ChatService chatService;



    @PostMapping("/conversations")

    @ResponseStatus(HttpStatus.CREATED)

    public ConversationResponse start(@Valid @RequestBody StartConversationRequest request) {

        return chatService.startConversation(request.getProductId());

    }



    @GetMapping("/conversations")

    public List<ConversationResponse> listConversations() {

        return chatService.listMyConversations();

    }



    @GetMapping("/conversations/{id}")

    public ConversationResponse getConversation(@PathVariable UUID id) {

        return chatService.getConversation(id);

    }



    @GetMapping("/conversations/{id}/messages")

    public List<ChatMessageResponse> listMessages(@PathVariable UUID id) {

        return chatService.listMessages(id);

    }



    @PostMapping("/conversations/{id}/messages")

    @ResponseStatus(HttpStatus.CREATED)

    public ChatMessageResponse sendMessage(@PathVariable UUID id, @Valid @RequestBody SendMessageRequest request) {

        return chatService.sendMessage(id, request.getBody(), request.getAttachmentUrl());

    }



    @PostMapping("/conversations/{id}/read")

    @ResponseStatus(HttpStatus.NO_CONTENT)

    public void markRead(@PathVariable UUID id) {

        chatService.markRead(id);

    }



    @PostMapping("/conversations/{id}/deal")

    @ResponseStatus(HttpStatus.NO_CONTENT)

    public void setDeal(@PathVariable UUID id, @Valid @RequestBody DealStatusRequest request) {

        chatService.setDealStatus(id, request.getStatus());

    }

}

