package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kumbu.backend.domain.enums.ConversationType;
import com.kumbu.backend.domain.enums.SupportStatus;

import java.time.Instant;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByProductIdAndBuyerId(String productId, UUID buyerId);

    List<Conversation> findByBuyerIdOrSellerIdOrderByUpdatedAtDesc(UUID buyerId, UUID sellerId);

    @Query("""
            SELECT c FROM Conversation c
            WHERE (c.buyerId = :userId OR c.sellerId = :userId)
              AND c.conversationType = com.kumbu.backend.domain.enums.ConversationType.MARKETPLACE
            ORDER BY c.updatedAt DESC
            """)
    List<Conversation> findMarketplaceByParticipantOrderByUpdatedAtDesc(@Param("userId") UUID userId);

    Optional<Conversation> findByBuyerIdAndConversationType(UUID buyerId, ConversationType conversationType);

    Optional<Conversation> findByGuestAccessTokenAndConversationType(
            String guestAccessToken, ConversationType conversationType);

    Page<Conversation> findByConversationTypeAndSupportStatusOrderByUpdatedAtDesc(
            ConversationType conversationType,
            com.kumbu.backend.domain.enums.SupportStatus supportStatus,
            Pageable pageable);

    Page<Conversation> findByConversationTypeOrderByUpdatedAtDesc(
            ConversationType conversationType,
            Pageable pageable);

    long countByConversationTypeAndSupportStatus(ConversationType conversationType, SupportStatus supportStatus);

    Optional<Conversation> findByIdAndBuyerIdOrSellerId(UUID id, UUID buyerId, UUID sellerId);

    Page<Conversation> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    Page<Conversation> findByBlockedTrueOrderByUpdatedAtDesc(Pageable pageable);

    @Query("""
            SELECT COUNT(c) FROM Conversation c
            JOIN CatalogProduct p ON c.productId = p.id
            WHERE p.categoryId = :categoryId
              AND c.createdAt >= :since
            """)
    long countByCategorySince(@Param("categoryId") String categoryId, @Param("since") Instant since);

    boolean existsByProductIdAndCreatedAtGreaterThanEqual(String productId, Instant since);
}
