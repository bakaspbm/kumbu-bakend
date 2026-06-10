package com.kumbu.backend.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminPaymentMethodCreateRequest {

    @NotBlank(message = "ID é obrigatório")
    @Size(max = 64, message = "ID inválido")
    private String id;

    @NotBlank(message = "Label é obrigatório")
    @Size(max = 120, message = "Label demasiado longo")
    private String label;

    @Size(max = 64, message = "Icon key inválida")
    private String iconKey;

    @Min(value = 0, message = "Ordem inválida")
    private Integer sortOrder;

    private Boolean isDefault;
}
