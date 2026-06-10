package com.kumbu.backend.dto.monetization;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminUpdateMonetizationSettingsRequest {

    private String companyName;
    private String referencePrefix;
    private Integer gateMinDau;
    private Integer gateMinListings;
    private Integer gateMinChats;
    private Integer defaultMaxListings;
    private Integer paymentExpiryHours;
    private Boolean chargingEnabled;
    private String defaultPaymentProviderId;
    private Integer paymentSlaHours;
    private Boolean bankTransfersEnabled;
    private Integer leadsMinServicosChats;
}
