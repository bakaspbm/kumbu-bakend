package com.kumbu.backend.dto.job;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class JobApplyRequest {

    @NotNull(message = "CV é obrigatório")
    private UUID cvId;

    @Size(max = 4000, message = "Mensagem de candidatura demasiado longa")
    private String coverMessage;
}
