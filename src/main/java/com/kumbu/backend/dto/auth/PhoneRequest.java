package com.kumbu.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PhoneRequest {

    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "^[+]?[0-9]{8,15}$", message = "Telefone inválido")
    private String phone;
}
