package com.kumbu.backend.controller;



import com.kumbu.backend.service.AdminManagementService;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.util.Map;



@RestController

@RequestMapping("/api/v1/admin/reviews")

@RequiredArgsConstructor

@Validated

public class AdminReviewsController {



    private final AdminManagementService adminManagementService;



    @GetMapping

    public Map<String, Object> list(

            @RequestParam(required = false) String product_id,

            @RequestParam(defaultValue = "0") @Min(0) int page,

            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        return adminManagementService.listReviews(page, size, product_id);

    }



    @DeleteMapping("/{id}")

    public void delete(@PathVariable java.util.UUID id) {

        adminManagementService.deleteReview(id);

    }

}

