package com.kumbu.backend.dto.monetization;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdatePaymentProviderRequest {

    private String name;
    private String providerType;
    private String bankName;
    private String accountHolder;
    private String accountNumber;
    private String iban;
    private String phoneNumber;
    private String instructions;
    private Boolean active;
    private Integer sortOrder;
}
