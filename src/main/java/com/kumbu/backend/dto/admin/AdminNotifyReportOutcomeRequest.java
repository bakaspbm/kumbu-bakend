package com.kumbu.backend.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.kumbu.backend.validation.OneOf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminNotifyReportOutcomeRequest {

    @NotBlank(message = "Estado é obrigatório")
    @OneOf(value = {"reviewing", "resolved", "dismissed"}, message = "Estado inválido")
    private String status;

    @Size(max = 4000, message = "Nota demasiado longa")
    private String adminNote;

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título demasiado longo")
    private String title;

    @NotBlank(message = "Corpo é obrigatório")
    @Size(max = 4000, message = "Corpo demasiado longo")
    private String body;
}
