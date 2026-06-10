package com.kumbu.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TokenRequest {

    @NotBlank(message = "Token é obrigatório")
    private String token;
}
