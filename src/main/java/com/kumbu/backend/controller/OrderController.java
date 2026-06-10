package com.kumbu.backend.controller;



import com.kumbu.backend.dto.order.CheckoutRequest;

import com.kumbu.backend.dto.order.OrderResponse;

import com.kumbu.backend.dto.order.OrderStatusRequest;

import com.kumbu.backend.service.OrderService;

import jakarta.validation.Valid;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.util.List;



@RestController

@RequestMapping("/api/v1/orders")

@RequiredArgsConstructor

@Validated

public class OrderController {



    private final OrderService orderService;



    @PostMapping("/checkout")

    public CheckoutResponse checkout(@Valid @RequestBody CheckoutRequest request) {

        OrderService.CheckoutResult result = orderService.checkout(request.getItems());

        return new CheckoutResponse(

                result.orders(),

                result.conversationIds(),

                result.orders().stream().map(OrderResponse::getId).toList()

        );

    }



    @GetMapping("/purchases")

    public List<OrderResponse> purchases() {

        return orderService.listPurchases();

    }



    @GetMapping("/sales")

    public List<OrderResponse> sales() {

        return orderService.listSales();

    }



    @GetMapping("/{id}")

    public OrderResponse getOrder(@PathVariable @NotBlank @Size(max = 64) String id) {

        return orderService.getOrder(id);

    }



    @PatchMapping("/{id}/status")

    public OrderResponse updateStatus(

            @PathVariable @NotBlank @Size(max = 64) String id,

            @Valid @RequestBody OrderStatusRequest request) {

        return orderService.updateStatus(id, request.getStatus());

    }



    public record CheckoutResponse(

            List<OrderResponse> orders,

            List<String> conversationIds,

            List<String> orderIds

    ) {}

}

