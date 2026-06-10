package com.kumbu.backend.dto.admin;

import com.kumbu.backend.validation.OneOf;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminUpdateRoleRequest {

    @NotBlank(message = "Role é obrigatória")
    @OneOf(value = {"super_admin", "admin", "support"}, message = "Role inválida")
    private String role;
}
