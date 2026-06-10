package com.kumbu.backend.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CheckoutRequest {

    @NotEmpty(message = "Carrinho não pode estar vazio")
    @Valid
    private List<CartItemRequest> items;
}
