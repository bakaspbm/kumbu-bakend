package com.kumbu.backend.dto.rental;

import com.kumbu.backend.validation.OneOf;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateRentalRequest {

    @NotBlank(message = "ID do imóvel é obrigatório")
    private String productId;

    @NotBlank(message = "Modo de aluguer é obrigatório")
    @OneOf(value = {"daily", "monthly", "sale"}, message = "Modo de aluguer inválido")
    private String rentalMode;

    private LocalDate checkIn;
    private LocalDate checkOut;

    @Size(max = 2000, message = "Mensagem demasiado longa")
    private String guestMessage;

    @Size(max = 120, message = "Preço inválido")
    private String priceSnapshot;
}
