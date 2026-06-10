package com.kumbu.backend.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.kumbu.backend.validation.OneOf;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminCategoryCreateRequest {

    @NotBlank(message = "ID é obrigatório")
    @Size(max = 64, message = "ID inválido")
    private String id;

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 120, message = "Nome demasiado longo")
    private String name;

    @Size(max = 64, message = "Icon key inválida")
    private String iconKey;

    @Pattern(regexp = "^[0-9A-Fa-f]{6}$", message = "Cor inválida")
    private String accentHex;

    @Min(value = 0, message = "Ordem inválida")
    private Integer sortOrder;

    @OneOf(value = {"product", "property", "job"}, message = "Tipo de categoria inválido")
    private String kind;
}
