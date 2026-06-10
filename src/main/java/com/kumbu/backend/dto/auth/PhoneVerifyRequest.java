package com.kumbu.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PhoneVerifyRequest {

    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "^[+]?[0-9]{8,15}$", message = "Telefone inválido")
    private String phone;

    @NotBlank(message = "Código é obrigatório")
    @Pattern(regexp = "^[0-9]{4,8}$", message = "Código inválido")
    private String token;
}
