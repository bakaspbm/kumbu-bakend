package com.kumbu.backend.dto.compliance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecordConsentRequest {

    @NotBlank(message = "Tipo de consentimento é obrigatório")
    @Size(max = 64)
    private String consentType;

    @Size(max = 512)
    private String userAgent;
}
