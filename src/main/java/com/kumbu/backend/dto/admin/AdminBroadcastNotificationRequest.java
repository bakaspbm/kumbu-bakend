package com.kumbu.backend.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminBroadcastNotificationRequest {

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título demasiado longo")
    private String title;

    @NotBlank(message = "Corpo é obrigatório")
    @Size(max = 4000, message = "Corpo demasiado longo")
    private String body;

    @Size(max = 64, message = "Icon key inválida")
    private String iconKey;

    @Size(max = 500, message = "URL de acção inválida")
    private String actionUrl;

    @Size(max = 32, message = "Audiência inválida")
    private String audience;

    private java.util.UUID userId;
}
