package com.kumbu.backend.dto.monetization;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InitiatePaymentRequest {

    @NotBlank
    private String productId;

    @NotBlank
    private String providerId;

    private String targetType;

    private String targetId;
}
