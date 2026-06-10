package com.kumbu.backend.dto.monetization;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewPaymentRequest {

    private String note;
    private String reason;
}
