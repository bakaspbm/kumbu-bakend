package com.kumbu.backend.dto.catalog;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class UpdateListingRequest {

    @Size(min = 3, max = 200, message = "Título deve ter entre 3 e 200 caracteres")
    private String title;

    @Size(max = 120, message = "Preço inválido")
    private String priceLabel;

    @Size(max = 8000, message = "Descrição demasiado longa")
    private String description;

    @Size(max = 120, message = "Texto de entrega inválido")
    private String deliveryText;

    private Boolean outOfStock;

    private List<@Size(max = 500) String> imageUrls;

    private Map<String, Object> productMeta;
    private Map<String, Object> propertyMeta;
    private Map<String, Object> jobMeta;
}
