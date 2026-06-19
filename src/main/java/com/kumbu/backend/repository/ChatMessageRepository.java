package com.kumbu.backend.repository;

import com.kumbu.backend.domain.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByConversationIdAndHiddenAtIsNullOrderByCreatedAtAsc(UUID conversationId);

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    @Modifying
    @Query("""
            UPDATE ChatMessage m SET m.readAt = :now
            WHERE m.conversationId = :conversationId
              AND m.senderId <> :userId
              AND m.readAt IS NULL
              AND m.hiddenAt IS NULL
            """)
    int markAsRead(@Param("conversationId") UUID conversationId,
                   @Param("userId") UUID userId,
                   @Param("now") Instant now);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.createdAt >= :since AND m.hiddenAt IS NULL")
    long countSince(@Param("since") Instant since);

    @Query("""
            SELECT COUNT(m) FROM ChatMessage m
            WHERE m.conversationId = :conversationId
              AND m.senderId <> :userId
              AND m.readAt IS NULL
              AND m.hiddenAt IS NULL
            """)
    long countUnreadForUser(@Param("conversationId") UUID conversationId, @Param("userId") UUID userId);

    java.util.Optional<ChatMessage> findTopByConversationIdAndHiddenAtIsNullOrderByCreatedAtDesc(UUID conversationId);

    @Query("SELECT COUNT(DISTINCT m.senderId) FROM ChatMessage m WHERE m.createdAt >= :since AND m.hiddenAt IS NULL")
    long countDistinctSendersSince(@Param("since") Instant since);

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM ChatMessage m
            JOIN Conversation c ON c.id = m.conversationId
            WHERE m.attachmentUrl = :attachmentUrl
              AND (c.buyerId = :userId OR c.sellerId = :userId)
            """)
    boolean existsByAttachmentUrlAndParticipant(@Param("attachmentUrl") String attachmentUrl,
                                                @Param("userId") UUID userId);
}
