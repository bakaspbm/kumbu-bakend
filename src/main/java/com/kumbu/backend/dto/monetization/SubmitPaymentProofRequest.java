package com.kumbu.backend.dto.monetization;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitPaymentProofRequest {

    private String proofUrl;
    private String proofNote;
}
