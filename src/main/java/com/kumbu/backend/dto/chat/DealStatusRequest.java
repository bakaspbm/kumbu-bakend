package com.kumbu.backend.dto.chat;

import com.kumbu.backend.validation.OneOf;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DealStatusRequest {

    @NotBlank(message = "Estado do negócio é obrigatório")
    @OneOf(value = {"open", "purchased", "rejected"}, message = "Estado do negócio inválido")
    private String status;
}
