package com.kumbu.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequest {

    @NotBlank(message = "Mensagem é obrigatória")
    @Size(min = 1, max = 4000, message = "Mensagem deve ter entre 1 e 4000 caracteres")
    private String body;
}
