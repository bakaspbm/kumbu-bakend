package com.kumbu.backend.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminMarketingBlockCreateRequest {

    @NotBlank(message = "ID é obrigatório")
    @Size(max = 64, message = "ID inválido")
    private String id;

    @NotBlank(message = "Tipo é obrigatório")
    @Size(max = 64, message = "Tipo inválido")
    private String kind;

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título demasiado longo")
    private String title;

    @Size(max = 300, message = "Subtítulo demasiado longo")
    private String subtitle;

    @NotBlank(message = "Cor inicial é obrigatória")
    @Size(max = 32, message = "Cor inválida")
    private String gradientFrom;

    @NotBlank(message = "Cor final é obrigatória")
    @Size(max = 32, message = "Cor inválida")
    private String gradientTo;

    @Min(value = 0, message = "Ordem inválida")
    private Integer sortOrder;
}
