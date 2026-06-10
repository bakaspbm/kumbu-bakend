package com.kumbu.backend.dto.admin;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AdminProductUpdateRequest {

    @Size(max = 200, message = "Título demasiado longo")
    private String title;

    @Size(max = 64, message = "Categoria inválida")
    private String categoryId;

    @Size(max = 64, message = "Subcategoria inválida")
    private String subcategoryId;

    @Size(max = 120, message = "Preço inválido")
    private String priceLabel;

    @Size(max = 120, message = "Preço anterior inválido")
    private String oldPriceLabel;

    @Size(max = 120, message = "Texto de entrega inválido")
    private String deliveryText;

    @Size(max = 8000, message = "Descrição demasiado longa")
    private String description;

    @Min(value = 0, message = "Ordem inválida")
    private Integer sortOrder;
}
