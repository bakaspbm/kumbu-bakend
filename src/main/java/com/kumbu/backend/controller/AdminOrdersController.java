package com.kumbu.backend.controller;



import com.kumbu.backend.dto.admin.AdminOrderStatusRequest;

import com.kumbu.backend.service.AdminManagementService;

import jakarta.validation.Valid;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.util.Map;
import java.util.UUID;



@RestController

@RequestMapping("/api/v1/admin/orders")

@RequiredArgsConstructor

@Validated

public class AdminOrdersController {



    private final AdminManagementService adminManagementService;

    @GetMapping
    public Map<String, Object> listOrders(
            @RequestParam(required = false) @Min(1) @Max(200) Integer limit,
            @RequestParam(required = false) @Min(1) @Max(365) Integer timeline_days,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID user_id,
            @RequestParam(required = false) UUID seller_id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return adminManagementService.listOrdersAdmin(
                limit, timeline_days, status, user_id, seller_id, page, size);
    }

    @PatchMapping("/{id}/status")

    public Map<String, Object> updateStatus(

            @PathVariable @NotBlank @Size(max = 64) String id,

            @Valid @RequestBody AdminOrderStatusRequest request) {

        return adminManagementService.updateOrderStatus(id, request.getStatus());

    }



    @DeleteMapping("/{id}")

    public void deleteOrder(@PathVariable @NotBlank @Size(max = 64) String id) {

        adminManagementService.deleteOrder(id);

    }

}

