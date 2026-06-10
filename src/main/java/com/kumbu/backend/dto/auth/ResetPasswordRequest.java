package com.kumbu.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Token é obrigatório")
    private String token;

    @NotBlank(message = "Password é obrigatória")
    @Size(min = 8, max = 72, message = "Password deve ter entre 8 e 72 caracteres")
    private String password;
}
