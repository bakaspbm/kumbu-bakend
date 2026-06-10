package com.kumbu.backend.controller;

import com.kumbu.backend.service.ChatService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat/{conversationId}/send")
    public void send(@DestinationVariable UUID conversationId, WsMessage payload) {
        chatService.sendMessage(conversationId, payload.getBody());
    }

    @Data
    public static class WsMessage {
        private String body;
    }
}
