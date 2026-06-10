package com.kumbu.backend.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminSubcategoryUpdateRequest {

    @Size(max = 120, message = "Label demasiado longo")
    private String label;

    @Min(value = 0, message = "Ordem inválida")
    private Integer sortOrder;
}
