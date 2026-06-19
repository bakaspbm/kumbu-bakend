package com.kumbu.backend.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminIdentityDocumentReviewRequest {

    @NotBlank(message = "Indique o motivo da rejeição.")
    @Size(max = 500, message = "Motivo demasiado longo.")
    private String note;
}
