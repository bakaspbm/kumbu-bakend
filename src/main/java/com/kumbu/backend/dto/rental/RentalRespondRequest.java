package com.kumbu.backend.dto.rental;

import com.kumbu.backend.validation.OneOf;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RentalRespondRequest {

    @NotBlank(message = "Acção é obrigatória")
    @OneOf(value = {"confirm", "confirmed", "reject", "rejected"}, message = "Acção inválida")
    private String action;
}
