package com.kumbu.backend.dto.catalog;

import com.kumbu.backend.validation.OneOf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class CreateListingRequest {

    @NotBlank(message = "Categoria é obrigatória")
    @Size(max = 64, message = "Categoria inválida")
    private String categoryId;

    @Size(max = 64, message = "Subcategoria inválida")
    private String subcategoryId;

    @NotBlank(message = "Título é obrigatório")
    @Size(min = 3, max = 200, message = "Título deve ter entre 3 e 200 caracteres")
    private String title;

    @NotBlank(message = "Preço é obrigatório")
    @Size(max = 120, message = "Preço inválido")
    private String priceLabel;

    @Size(max = 8000, message = "Descrição demasiado longa")
    private String description;

    @Size(max = 120, message = "Texto de entrega inválido")
    private String deliveryText;

    @OneOf(value = {"general", "product", "property", "job"}, message = "Tipo de anúncio inválido")
    private String listingKind = "general";

    private List<@NotBlank @Size(max = 500) String> imageUrls;
    private Map<String, Object> productMeta;
    private Map<String, Object> propertyMeta;
    private Map<String, Object> jobMeta;
}
