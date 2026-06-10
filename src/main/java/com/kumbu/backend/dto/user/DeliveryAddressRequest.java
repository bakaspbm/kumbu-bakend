package com.kumbu.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliveryAddressRequest {

    @NotBlank(message = "Nome do destinatário é obrigatório")
    @Size(max = 120, message = "Nome do destinatário demasiado longo")
    private String recipientName;

    @NotBlank(message = "Telefone é obrigatório")
    @Size(max = 30, message = "Telefone inválido")
    private String phone;

    @NotBlank(message = "Morada é obrigatória")
    @Size(max = 255, message = "Morada demasiado longa")
    private String street;

    @Size(max = 120, message = "Cidade demasiado longa")
    private String city;

    @Size(max = 120, message = "Região demasiado longa")
    private String region;

    @Size(max = 120, message = "País demasiado longo")
    private String country;

    @Size(max = 20, message = "Código postal inválido")
    private String postalCode;
}
