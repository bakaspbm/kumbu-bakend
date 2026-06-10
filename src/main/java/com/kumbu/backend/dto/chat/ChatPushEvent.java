package com.kumbu.backend.dto.chat;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatPushEvent {
    /** "new" — nova mensagem na conversa. */
    private String type;
    private ChatMessageResponse message;
}
