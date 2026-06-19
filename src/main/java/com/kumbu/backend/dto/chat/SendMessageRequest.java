package com.kumbu.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

    @Size(max = 4000, message = "Mensagem demasiado longa")
    private String body;

    @Size(max = 512)
    private String attachmentUrl;
}
