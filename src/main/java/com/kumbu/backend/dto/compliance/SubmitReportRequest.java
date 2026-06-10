package com.kumbu.backend.dto.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class SubmitReportRequest {

    @NotBlank(message = "Tipo de alvo é obrigatório")
    @Size(max = 32)
    private String targetType;

    @NotBlank(message = "Identificador do alvo é obrigatório")
    @Size(max = 128)
    private String targetId;

    private UUID reportedUserId;

    @NotBlank(message = "Motivo é obrigatório")
    @Size(max = 40)
    private String reason;

    @Size(max = 2000)
    private String details;
}
