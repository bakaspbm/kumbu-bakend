package com.kumbu.backend.controller;



import com.kumbu.backend.dto.user.DeliveryAddressRequest;

import com.kumbu.backend.dto.user.SyncCartRequest;

import com.kumbu.backend.dto.user.UpdateProfileRequest;

import com.kumbu.backend.dto.user.UserProfileResponse;

import com.kumbu.backend.service.UserService;
import com.kumbu.backend.service.UserPresenceService;
import com.kumbu.backend.service.AccountDataService;

import jakarta.validation.Valid;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.util.Map;

import java.util.UUID;



@RestController

@RequestMapping("/api/v1/users")

@RequiredArgsConstructor

@Validated

public class UserController {



    private final UserService userService;
    private final UserPresenceService userPresenceService;
    private final AccountDataService accountDataService;



    @GetMapping("/me")

    public UserProfileResponse me() {

        return userService.me();

    }



    @GetMapping("/{id}")

    public UserProfileResponse getProfile(@PathVariable UUID id) {

        return userService.getProfile(id);

    }



    @GetMapping("/me/publish-readiness")

    public Map<String, Object> publishReadiness() {

        return userService.publishReadiness();

    }



    @PatchMapping("/me")

    public UserProfileResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {

        return userService.updateProfile(request);

    }



    @PutMapping("/me/delivery-address")

    public UserProfileResponse updateDeliveryAddress(@Valid @RequestBody DeliveryAddressRequest request) {

        return userService.updateDeliveryAddress(request);

    }



    @PutMapping("/me/cart")

    public void syncCart(@Valid @RequestBody SyncCartRequest request) {

        userService.syncCart(request.getItems());

    }



    @PostMapping("/me/favorites/{productId}")

    public void addFavorite(@PathVariable @NotBlank @Size(max = 64) String productId) {

        userService.addFavorite(productId);

    }



    @PostMapping("/me/presence")

    public void touchPresence() {

        userPresenceService.touchCurrentUser();

    }



    @DeleteMapping("/me/favorites/{productId}")

    public void removeFavorite(@PathVariable @NotBlank @Size(max = 64) String productId) {

        userService.removeFavorite(productId);

    }



    @GetMapping("/me/export")

    public Map<String, Object> exportAccount() {

        return accountDataService.exportAccountData();

    }



    @DeleteMapping("/me")

    public void deleteAccount() {

        accountDataService.deleteAccount();

    }

}

