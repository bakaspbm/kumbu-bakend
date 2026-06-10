package com.kumbu.backend.dto.admin;

import com.kumbu.backend.validation.OneOf;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminOrderStatusRequest {

    @NotBlank(message = "Estado é obrigatório")
    @OneOf(value = {"processing", "shipping", "delivered", "cancelled"}, message = "Estado de encomenda inválido")
    private String status;
}
