package com.kumbu.backend.controller;



import com.kumbu.backend.dto.user.DeliveryAddressRequest;

import com.kumbu.backend.dto.user.SyncCartRequest;

import com.kumbu.backend.service.StoreService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;



import java.util.Map;



@RestController

@RequestMapping("/api/v1/store")

@RequiredArgsConstructor

public class StoreController {



    private final StoreService storeService;



    @GetMapping("/me")

    public StoreService.StoreUserDto me() {

        return storeService.getStoreUser();

    }



    @GetMapping("/payment-methods")
    public Map<String, Object> paymentMethods() {
        return Map.of(
                "items", storeService.listPaymentMethods(),
                "payment_context", Map.of(
                        "purpose", "MARKETPLACE_CHECKOUT",
                        "description", "Pagamento entre comprador e vendedor ao comprar um produto. O dinheiro vai para o vendedor.",
                        "not_for", "PLATFORM_FEATURE",
                        "platform_features_url", "/api/v1/monetization/catalog"
                )
        );
    }



    @PutMapping("/delivery-address")

    public StoreService.StoreUserDto deliveryAddress(@Valid @RequestBody DeliveryAddressRequest address) {

        return storeService.updateDeliveryAddress(address);

    }



    @PutMapping("/cart")

    public void syncCart(@Valid @RequestBody SyncCartRequest request) {

        storeService.syncCart(request.getItems());

    }

}

