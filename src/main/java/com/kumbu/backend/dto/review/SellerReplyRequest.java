package com.kumbu.backend.dto.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SellerReplyRequest {

    @NotBlank(message = "Resposta é obrigatória")
    @Size(min = 1, max = 2000, message = "Resposta deve ter entre 1 e 2000 caracteres")
    private String reply;
}
