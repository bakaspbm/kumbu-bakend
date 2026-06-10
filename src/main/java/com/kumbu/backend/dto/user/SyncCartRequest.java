package com.kumbu.backend.dto.user;

import com.kumbu.backend.dto.order.CartItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SyncCartRequest {

    @NotNull(message = "Carrinho é obrigatório")
    @Valid
    private List<CartItemRequest> items;
}
