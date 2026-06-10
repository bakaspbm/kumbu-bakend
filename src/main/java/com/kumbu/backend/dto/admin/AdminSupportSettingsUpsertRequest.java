package com.kumbu.backend.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminSupportSettingsUpsertRequest {

    @Size(max = 4000, message = "Mensagem de boas-vindas demasiado longa")
    private String welcomeMessage;

    @Size(max = 120, message = "Email inválido")
    private String supportEmail;

    @Size(max = 30, message = "Telefone inválido")
    private String supportPhone;

    private List<Map<String, Object>> quickActions;
}
