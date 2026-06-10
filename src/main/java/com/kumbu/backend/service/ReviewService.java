package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.*;
import com.kumbu.backend.domain.enums.ConversationType;
import com.kumbu.backend.domain.enums.DealStatus;
import com.kumbu.backend.domain.enums.OrderStatus;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.*;
import com.kumbu.backend.security.SecurityUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final CatalogProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ConversationRepository conversationRepository;
    private final PropertyRentalRequestRepository rentalRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<ReviewDto> listProductReviews(String productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public boolean buyerCanReview(String productId) {
        UUID userId = securityUtils.currentUserId();
        if (reviewRepository.existsByProductIdAndUserId(productId, userId)) {
            return false;
        }

        CatalogProduct product = productRepository.findByIdAndDeletedAtIsNull(productId).orElse(null);
        if (product == null || userId.equals(product.getSellerId())) {
            return false;
        }

        if (hasPurchasedViaOrder(userId, productId)) {
            return true;
        }
        if (hasPurchasedViaChat(userId, productId)) {
            return true;
        }
        if (rentalRepository.existsByProductIdAndRenterIdAndStatus(productId, userId, "confirmed")) {
            return true;
        }
        return jobApplicationRepository.findByJobIdAndApplicantId(productId, userId)
                .map(app -> "accepted".equalsIgnoreCase(app.getStatus()))
                .orElse(false);
    }

    private boolean hasPurchasedViaOrder(UUID userId, String productId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(o -> o.getStatus() == OrderStatus.PROCESSING
                        || o.getStatus() == OrderStatus.SHIPPING
                        || o.getStatus() == OrderStatus.DELIVERED)
                .anyMatch(o -> orderItemRepository.existsByOrderIdAndProductId(o.getId(), productId));
    }

    private boolean hasPurchasedViaChat(UUID userId, String productId) {
        return conversationRepository.findByProductIdAndBuyerId(productId, userId)
                .filter(c -> c.getConversationType() != ConversationType.SUPPORT)
                .map(c -> c.getDealStatus() == DealStatus.PURCHASED)
                .orElse(false);
    }

    @Transactional
    public void submitReview(String productId, int rating, String comment) {
        UUID userId = securityUtils.currentUserId();
        if (!buyerCanReview(productId)) {
            throw ApiException.forbidden("Não pode avaliar este produto");
        }
        if (rating < 1 || rating > 5) {
            throw ApiException.badRequest("Rating inválido");
        }

        ProductReview review = ProductReview.builder()
                .productId(productId)
                .userId(userId)
                .rating(rating)
                .comment(comment)
                .build();
        reviewRepository.save(review);

        CatalogProduct product = productRepository.findById(productId).orElseThrow();
        product.setReviewCount(product.getReviewCount() + 1);
        recalculateProductRating(product);
        productRepository.save(product);
    }

    private void recalculateProductRating(CatalogProduct product) {
        List<ProductReview> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(product.getId());
        if (reviews.isEmpty()) {
            return;
        }
        double average = reviews.stream().mapToInt(ProductReview::getRating).average().orElse(0);
        product.setRating(java.math.BigDecimal.valueOf(average).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Transactional
    public void sellerReply(UUID reviewId, String reply) {
        UUID sellerId = securityUtils.currentUserId();
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> ApiException.notFound("Review não encontrada"));

        CatalogProduct product = productRepository.findById(review.getProductId())
                .orElseThrow(() -> ApiException.notFound("Produto não encontrado"));

        if (!sellerId.equals(product.getSellerId())) {
            throw ApiException.forbidden("Apenas o vendedor pode responder");
        }

        review.setSellerReply(reply);
        review.setSellerReplyAt(Instant.now());
        reviewRepository.save(review);
    }

    private ReviewDto toDto(ProductReview r) {
        User reviewer = userRepository.findById(r.getUserId()).orElse(null);
        return ReviewDto.builder()
                .id(r.getId())
                .productId(r.getProductId())
                .userId(r.getUserId())
                .rating(r.getRating())
                .comment(r.getComment())
                .sellerReply(r.getSellerReply())
                .sellerReplyAt(r.getSellerReplyAt())
                .reviewerName(reviewer != null ? reviewer.getDisplayName() : null)
                .reviewerPhotoUrl(reviewer != null ? reviewer.getPhotoUrl() : null)
                .createdAt(r.getCreatedAt())
                .build();
    }

    @Data
    @Builder
    public static class ReviewDto {
        private UUID id;
        private String productId;
        private UUID userId;
        private int rating;
        private String comment;
        private String sellerReply;
        private Instant sellerReplyAt;
        private String reviewerName;
        private String reviewerPhotoUrl;
        private Instant createdAt;
    }
}
