package com.kumbu.backend.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminToggleRequest {

    @NotNull(message = "Valor booleano é obrigatório")
    private Boolean value;
}
