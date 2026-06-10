package com.kumbu.backend.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StartConversationRequest {

    @NotBlank(message = "ID do produto é obrigatório")
    private String productId;
}
