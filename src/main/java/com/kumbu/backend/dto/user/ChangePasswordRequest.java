package com.kumbu.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Palavra-passe actual é obrigatória")
    private String currentPassword;

    @NotBlank(message = "Nova palavra-passe é obrigatória")
    @Size(min = 8, max = 128, message = "A nova palavra-passe deve ter pelo menos 8 caracteres")
    private String newPassword;
}
