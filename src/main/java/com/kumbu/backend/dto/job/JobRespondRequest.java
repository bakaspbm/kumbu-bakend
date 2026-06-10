package com.kumbu.backend.dto.job;

import com.kumbu.backend.validation.OneOf;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobRespondRequest {

    @NotBlank(message = "Acção é obrigatória")
    @OneOf(value = {"accept", "accepted", "reject", "rejected"}, message = "Acção inválida")
    private String action;
}
