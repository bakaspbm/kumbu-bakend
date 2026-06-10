package com.kumbu.backend.dto.admin;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminBlockConversationRequest {

    @Size(max = 500, message = "Motivo demasiado longo")
    private String reason;
}
