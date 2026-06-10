package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.*;
import com.kumbu.backend.domain.enums.MonetizationFeatureType;
import com.kumbu.backend.domain.enums.PromotionType;
import com.kumbu.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonetizationFulfillmentService {

    private final MonetizationProductRepository productRepository;
    private final CatalogProductRepository catalogProductRepository;
    private final ListingPromotionRepository listingPromotionRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final SellerVerificationRepository verificationRepository;
    private final ContactUnlockRepository contactUnlockRepository;

    @Transactional
    public void fulfill(MonetizationPayment payment) {
        MonetizationProduct product = productRepository.findById(payment.getProductId())
                .orElseThrow();

        switch (product.getFeatureType()) {
            case HIGHLIGHT_TOP, HIGHLIGHT_SIMPLE, HIGHLIGHT_URGENT, PREMIUM_HIGHLIGHT ->
                    applyListingHighlight(payment, product);
            case BOOST -> applyBoost(payment, product);
            case VIP_PLAN -> applyVipSubscription(payment, product);
            case BUSINESS_STARTER, BUSINESS_PRO -> applyBusinessPlan(payment, product);
            case SELLER_VERIFY_BASIC, SELLER_VERIFY_FULL -> applySellerVerification(payment, product);
            case CONTACT_FEE -> applyContactUnlock(payment);
            default -> { /* PAID_LEADS, ADVERTISING, JOB_POSTING — activar quando integrar front */ }
        }
    }

    private void applyListingHighlight(MonetizationPayment payment, MonetizationProduct product) {
        String listingId = payment.getTargetId();
        CatalogProduct listing = catalogProductRepository.findById(listingId).orElseThrow();

        int durationDays = product.getDurationDays() != null ? product.getDurationDays() : 7;
        Instant endsAt = Instant.now().plus(durationDays, ChronoUnit.DAYS);
        int boostScore = extractInt(product.getMetadata(), "boost_score", 10);
        PromotionType promoType = mapPromotionType(product.getFeatureType());

        listing.setFeatured(true);
        listing.setFeaturedUntil(endsAt);
        listing.setHighlightType(product.getFeatureType().name());
        listing.setBoostScore(Math.max(listing.getBoostScore(), boostScore));
        listing.setBoostedUntil(endsAt);
        listing.setSortOrder(listing.getSortOrder() - boostScore);
        catalogProductRepository.save(listing);

        listingPromotionRepository.save(ListingPromotion.builder()
                .productId(listingId)
                .userId(payment.getUserId())
                .paymentId(payment.getId())
                .promotionType(promoType)
                .startsAt(Instant.now())
                .endsAt(endsAt)
                .boostScore(boostScore)
                .build());
    }

    private void applyBoost(MonetizationPayment payment, MonetizationProduct product) {
        String listingId = payment.getTargetId();
        CatalogProduct listing = catalogProductRepository.findById(listingId).orElseThrow();

        int boostScore = extractInt(product.getMetadata(), "boost_score", 5);
        int boostCount = extractInt(product.getMetadata(), "boost_count", 1);
        Instant endsAt = Instant.now().plus(24L * boostCount, ChronoUnit.HOURS);

        listing.setBoostScore(listing.getBoostScore() + boostScore);
        listing.setBoostedUntil(endsAt);
        listing.setSortOrder(listing.getSortOrder() - boostScore);
        catalogProductRepository.save(listing);

        listingPromotionRepository.save(ListingPromotion.builder()
                .productId(listingId)
                .userId(payment.getUserId())
                .paymentId(payment.getId())
                .promotionType(PromotionType.BOOST)
                .startsAt(Instant.now())
                .endsAt(endsAt)
                .boostScore(boostScore)
                .build());
    }

    private void applyVipSubscription(MonetizationPayment payment, MonetizationProduct product) {
        User user = userRepository.findById(payment.getUserId()).orElseThrow();
        int durationDays = product.getDurationDays() != null ? product.getDurationDays() : 30;
        int maxListings = product.getMaxListings() != null ? product.getMaxListings() : 15;

        Instant startsAt = Instant.now();
        Instant endsAt = user.isVipActive() && user.getVipUntil() != null
                ? user.getVipUntil().plus(durationDays, ChronoUnit.DAYS)
                : startsAt.plus(durationDays, ChronoUnit.DAYS);

        user.setVipUntil(endsAt);
        user.setMaxListings(Math.max(user.getMaxListings(), maxListings));
        userRepository.save(user);

        subscriptionRepository.save(UserSubscription.builder()
                .userId(payment.getUserId())
                .productId(product.getId())
                .paymentId(payment.getId())
                .planType("VIP")
                .status("ACTIVE")
                .maxListings(maxListings)
                .startsAt(startsAt)
                .endsAt(endsAt)
                .build());
    }

    private void applyBusinessPlan(MonetizationPayment payment, MonetizationProduct product) {
        User user = userRepository.findById(payment.getUserId()).orElseThrow();
        int durationDays = product.getDurationDays() != null ? product.getDurationDays() : 30;
        int maxListings = product.getMaxListings() != null ? product.getMaxListings() : 50;
        String planType = product.getFeatureType() == MonetizationFeatureType.BUSINESS_PRO ? "PRO" : "STARTER";

        Instant endsAt = Instant.now().plus(durationDays, ChronoUnit.DAYS);

        user.setBusinessPlanId(product.getId());
        user.setMaxListings(Math.max(user.getMaxListings(), maxListings));
        user.setVipUntil(endsAt);
        userRepository.save(user);

        subscriptionRepository.save(UserSubscription.builder()
                .userId(payment.getUserId())
                .productId(product.getId())
                .paymentId(payment.getId())
                .planType(planType)
                .status("ACTIVE")
                .maxListings(maxListings)
                .startsAt(Instant.now())
                .endsAt(endsAt)
                .build());
    }

    private void applySellerVerification(MonetizationPayment payment, MonetizationProduct product) {
        String tier = extractString(product.getMetadata(), "tier", "BASIC");
        Instant expiresAt = Instant.now().plus(365, ChronoUnit.DAYS);

        verificationRepository.save(SellerVerification.builder()
                .userId(payment.getUserId())
                .paymentId(payment.getId())
                .tier(tier)
                .status("APPROVED")
                .reviewedAt(Instant.now())
                .expiresAt(expiresAt)
                .build());

        User user = userRepository.findById(payment.getUserId()).orElseThrow();
        user.setSellerVerified(true);
        user.setSellerVerificationTier(tier);
        userRepository.save(user);
    }

    private void applyContactUnlock(MonetizationPayment payment) {
        UUID buyerId = payment.getUserId();
        String listingId = payment.getTargetId();

        CatalogProduct listing = catalogProductRepository.findById(listingId).orElseThrow();
        UUID sellerId = listing.getSellerId();

        if (!contactUnlockRepository.existsByBuyerIdAndSellerIdAndProductId(buyerId, sellerId, listingId)) {
            contactUnlockRepository.save(ContactUnlock.builder()
                    .buyerId(buyerId)
                    .sellerId(sellerId)
                    .productId(listingId)
                    .paymentId(payment.getId())
                    .build());
        }
    }

    private PromotionType mapPromotionType(MonetizationFeatureType type) {
        return switch (type) {
            case HIGHLIGHT_TOP -> PromotionType.HIGHLIGHT_TOP;
            case HIGHLIGHT_SIMPLE -> PromotionType.HIGHLIGHT_SIMPLE;
            case HIGHLIGHT_URGENT -> PromotionType.HIGHLIGHT_URGENT;
            case PREMIUM_HIGHLIGHT -> PromotionType.PREMIUM_HIGHLIGHT;
            default -> PromotionType.BOOST;
        };
    }

    private int extractInt(Map<String, Object> metadata, String key, int defaultValue) {
        if (metadata == null || !metadata.containsKey(key)) return defaultValue;
        Object val = metadata.get(key);
        if (val instanceof Number n) return n.intValue();
        return defaultValue;
    }

    private String extractString(Map<String, Object> metadata, String key, String defaultValue) {
        if (metadata == null || !metadata.containsKey(key)) return defaultValue;
        return String.valueOf(metadata.get(key));
    }
}
