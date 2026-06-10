package com.kumbu.backend.controller;



import com.kumbu.backend.dto.review.SellerReplyRequest;

import com.kumbu.backend.dto.review.SubmitReviewRequest;

import com.kumbu.backend.service.ReviewService;

import jakarta.validation.Valid;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;



import java.util.List;

import java.util.UUID;



@RestController

@RequestMapping("/api/v1/reviews")

@RequiredArgsConstructor

@Validated

public class ReviewController {



    private final ReviewService reviewService;



    @GetMapping("/products/{productId}")

    public List<ReviewService.ReviewDto> listProductReviews(@PathVariable @NotBlank @Size(max = 64) String productId) {

        return reviewService.listProductReviews(productId);

    }



    @GetMapping("/products/{productId}/can-review")

    public CanReviewResponse canReview(@PathVariable @NotBlank @Size(max = 64) String productId) {

        return new CanReviewResponse(reviewService.buyerCanReview(productId));

    }



    @PostMapping("/products/{productId}")

    @ResponseStatus(HttpStatus.CREATED)

    public void submit(

            @PathVariable @NotBlank @Size(max = 64) String productId,

            @Valid @RequestBody SubmitReviewRequest request) {

        reviewService.submitReview(productId, request.getRating(), request.getComment());

    }



    @PostMapping("/{reviewId}/reply")

    @ResponseStatus(HttpStatus.NO_CONTENT)

    public void sellerReply(@PathVariable UUID reviewId, @Valid @RequestBody SellerReplyRequest request) {

        reviewService.sellerReply(reviewId, request.getReply());

    }



    public record CanReviewResponse(boolean canReview) {}

}

