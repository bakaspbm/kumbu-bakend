package com.kumbu.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh token é obrigatório")
    private String refreshToken;
}
