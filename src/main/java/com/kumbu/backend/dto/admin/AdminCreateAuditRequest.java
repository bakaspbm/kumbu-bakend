package com.kumbu.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class AdminCreateAuditRequest {

    @NotBlank(message = "Acção é obrigatória")
    @Size(max = 120, message = "Acção demasiado longa")
    private String action;

    @NotBlank(message = "Entidade é obrigatória")
    @Size(max = 120, message = "Entidade demasiado longa")
    private String entity;

    @NotBlank(message = "ID da entidade é obrigatório")
    @Size(max = 120, message = "ID da entidade inválido")
    private String entityId;

    private Map<String, Object> payload;
}
