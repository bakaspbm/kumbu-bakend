package com.kumbu.backend.controller;



import com.kumbu.backend.dto.rental.CreateRentalRequest;

import com.kumbu.backend.dto.rental.RentalRespondRequest;

import com.kumbu.backend.service.RentalService;

import com.kumbu.backend.validation.OneOf;

import jakarta.validation.Valid;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;

import org.springframework.http.HttpStatus;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.time.LocalDate;

import java.util.List;

import java.util.UUID;



@RestController

@RequestMapping("/api/v1/rentals")

@RequiredArgsConstructor

@Validated

public class RentalController {



    private final RentalService rentalService;



    @GetMapping

    public List<RentalService.RentalDto> list(

            @RequestParam @NotBlank @OneOf(value = {"owner", "renter"}, message = "Role inválida") String role) {

        return rentalService.listMine(role);

    }



    @PostMapping

    @ResponseStatus(HttpStatus.CREATED)

    public RentalService.RentalDto create(@Valid @RequestBody CreateRentalRequest request) {

        return rentalService.create(request);

    }



    @PostMapping("/{id}/respond")

    public RentalService.RentalDto respond(@PathVariable UUID id, @Valid @RequestBody RentalRespondRequest request) {

        return rentalService.respond(id, request.getAction());

    }



    @GetMapping("/products/{productId}/occupied")

    public List<RentalService.DateRangeDto> occupied(@PathVariable @NotBlank @Size(max = 64) String productId) {

        return rentalService.occupiedRanges(productId);

    }



    @GetMapping("/products/{productId}/available")

    public AvailableResponse available(

            @PathVariable @NotBlank @Size(max = 64) String productId,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,

            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {

        return new AvailableResponse(rentalService.isDailyRangeAvailable(productId, checkIn, checkOut));

    }



    public record AvailableResponse(boolean available) {}

}

