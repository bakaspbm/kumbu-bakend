package com.kumbu.backend.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CartItemRequest {

    @NotBlank(message = "ID do produto é obrigatório")
    @Size(max = 64, message = "ID do produto inválido")
    private String productId;

    @NotBlank(message = "ID do vendedor é obrigatório")
    private String sellerId;

    @Min(value = 1, message = "Quantidade mínima é 1")
    private int quantity = 1;

    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título inválido")
    private String title;

    @NotBlank(message = "Preço é obrigatório")
    @Size(max = 120, message = "Preço inválido")
    private String priceLabel;

    @Size(max = 500, message = "URL da imagem inválida")
    private String imageUrl;
}
