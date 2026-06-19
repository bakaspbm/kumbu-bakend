package com.kumbu.backend.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.kumbu.backend.validation.OneOf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminUpdateReportRequest {

    @NotBlank(message = "Estado é obrigatório")
    @OneOf(value = {"reviewing", "resolved", "dismissed"}, message = "Estado de report inválido")
    private String status;

    @Size(max = 4000, message = "Notas demasiado longas")
    private String adminNotes;
}
