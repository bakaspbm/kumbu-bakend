package com.kumbu.backend.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.kumbu.backend.validation.OneOf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminCreateAdminRequest {

    @NotNull(message = "Utilizador é obrigatório")
    private UUID userId;

    @NotBlank(message = "Role é obrigatória")
    @OneOf(value = {"super_admin", "admin", "support"}, message = "Role inválida")
    private String role;
}
