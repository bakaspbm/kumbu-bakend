package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.CatalogProduct;
import com.kumbu.backend.domain.entity.ChatMessage;
import com.kumbu.backend.domain.entity.Conversation;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.enums.ConversationType;
import com.kumbu.backend.domain.enums.DealStatus;
import com.kumbu.backend.domain.enums.MonetizationFeatureType;
import com.kumbu.backend.dto.chat.ChatMessageResponse;
import com.kumbu.backend.dto.chat.ChatPushEvent;
import com.kumbu.backend.dto.chat.ConversationResponse;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.*;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private static final String USER_MESSAGES_DEST = "/queue/messages";

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CatalogProductRepository productRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final MonetizationPhaseService monetizationPhaseService;
    private final ContactUnlockRepository contactUnlockRepository;
    private final MonetizationVipTrackingService vipTrackingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserPresenceService userPresenceService;

    @Transactional
    public ConversationResponse startConversation(String productId) {
        UUID buyerId = securityUtils.currentUserId();
        CatalogProduct product = productRepository.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> ApiException.notFound("Produto não encontrado"));

        if (product.getSellerId() == null || product.getSellerId().equals(buyerId)) {
            throw ApiException.badRequest("Não pode iniciar conversa consigo mesmo");
        }

        if (monetizationPhaseService.isFeatureActive(MonetizationFeatureType.CONTACT_FEE)
                && !contactUnlockRepository.existsByBuyerIdAndSellerIdAndProductId(
                        buyerId, product.getSellerId(), productId)) {
            throw ApiException.forbidden("Desbloqueie o contacto do vendedor (100 Kz) antes de iniciar conversa");
        }

        Conversation conversation = conversationRepository.findByProductIdAndBuyerId(productId, buyerId)
                .orElseGet(() -> {
                    Conversation created = conversationRepository.save(Conversation.builder()
                            .productId(productId)
                            .buyerId(buyerId)
                            .sellerId(product.getSellerId())
                            .dealStatus(DealStatus.OPEN)
                            .build());
                    vipTrackingService.recordVipContact(product.getSellerId(), product.getCategoryId());
                    return created;
                });

        return toConversation(conversation, buyerId);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID conversationId) {
        UUID userId = securityUtils.currentUserId();
        Conversation conversation = conversationRepository.findById(conversationId)
                .filter(c -> c.getConversationType() != ConversationType.SUPPORT)
                .filter(c -> userId.equals(c.getBuyerId()) || userId.equals(c.getSellerId()))
                .orElseThrow(() -> ApiException.notFound("Conversa não encontrada"));
        return toConversation(conversation, userId);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listMyConversations() {
        UUID userId = securityUtils.currentUserId();
        return conversationRepository.findMarketplaceByParticipantOrderByUpdatedAtDesc(userId).stream()
                .map(c -> toConversation(c, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> listMessages(UUID conversationId) {
        assertParticipant(conversationId);
        return chatMessageRepository.findByConversationIdAndHiddenAtIsNullOrderByCreatedAtAsc(conversationId)
                .stream().map(this::toMessage).toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID conversationId, String body) {
        if (body == null || body.isBlank()) {
            throw ApiException.badRequest("Mensagem é obrigatória");
        }
        if (body.length() > 4000) {
            throw ApiException.badRequest("Mensagem demasiado longa");
        }
        UUID senderId = securityUtils.currentUserId();
        Conversation conversation = assertParticipant(conversationId);

        if (conversation.isBlocked()) {
            throw ApiException.forbidden("Conversa bloqueada");
        }

        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .body(body.trim())
                .build());

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        ChatMessageResponse response = toMessage(message);
        schedulePushMessage(conversation, senderId, response);
        return response;
    }

    @Transactional
    public void markRead(UUID conversationId) {
        UUID userId = securityUtils.currentUserId();
        assertParticipant(conversationId);
        chatMessageRepository.markAsRead(conversationId, userId, Instant.now());
    }

    @Transactional
    public void setDealStatus(UUID conversationId, String status) {
        UUID userId = securityUtils.currentUserId();
        Conversation conversation = assertParticipant(conversationId);

        if (!userId.equals(conversation.getBuyerId())) {
            throw ApiException.forbidden("Apenas o comprador pode alterar o estado do negócio");
        }

        DealStatus dealStatus;
        try {
            dealStatus = DealStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Estado do negócio inválido");
        }
        conversation.setDealStatus(dealStatus);
        conversation.setDealStatusAt(Instant.now());
        conversation.setDealStatusBy(userId);
        conversationRepository.save(conversation);
    }

    private Conversation assertParticipant(UUID conversationId) {
        UUID userId = securityUtils.currentUserId();
        return conversationRepository.findById(conversationId)
                .filter(c -> c.getConversationType() != ConversationType.SUPPORT)
                .filter(c -> userId.equals(c.getBuyerId()) || userId.equals(c.getSellerId()))
                .orElseThrow(() -> ApiException.notFound("Conversa não encontrada"));
    }

    private ConversationResponse toConversation(Conversation c, UUID currentUserId) {
        UUID otherId = currentUserId.equals(c.getBuyerId()) ? c.getSellerId() : c.getBuyerId();
        User other = userRepository.findById(otherId).orElse(null);
        String productTitle = productRepository.findById(c.getProductId()).map(CatalogProduct::getTitle).orElse(null);

        ChatMessage lastMessage = chatMessageRepository
                .findTopByConversationIdAndHiddenAtIsNullOrderByCreatedAtDesc(c.getId())
                .orElse(null);
        long unread = chatMessageRepository.countUnreadForUser(c.getId(), currentUserId);
        Instant otherLastSeen = other != null ? other.getLastSeenAt() : null;

        return ConversationResponse.builder()
                .id(c.getId())
                .productId(c.getProductId())
                .productTitle(productTitle)
                .buyerId(c.getBuyerId())
                .sellerId(c.getSellerId())
                .otherPartyId(otherId)
                .otherPartyName(other != null ? other.getDisplayName() : null)
                .lastMessageBody(lastMessage != null ? lastMessage.getBody() : null)
                .lastMessageSenderId(lastMessage != null ? lastMessage.getSenderId() : null)
                .lastMessageAt(lastMessage != null ? lastMessage.getCreatedAt() : null)
                .updatedAt(c.getUpdatedAt())
                .dealStatus(c.getDealStatus().name().toLowerCase())
                .unreadCount((int) Math.min(unread, Integer.MAX_VALUE))
                .otherPartyLastSeenAt(otherLastSeen)
                .otherPartyOnline(userPresenceService.isOnline(otherLastSeen))
                .build();
    }

    private ChatMessageResponse toMessage(ChatMessage m) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .body(m.getBody())
                .createdAt(m.getCreatedAt())
                .readAt(m.getReadAt())
                .messageKind(m.getMessageKind())
                .fromSupport(false)
                .build();
    }

    private void schedulePushMessage(Conversation conversation, UUID senderId, ChatMessageResponse message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            pushMessage(conversation, senderId, message);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                pushMessage(conversation, senderId, message);
            }
        });
    }

    private void pushMessage(Conversation conversation, UUID senderId, ChatMessageResponse message) {
        try {
            messagingTemplate.convertAndSend("/topic/chat/" + conversation.getId(), message);
            UUID recipientId = senderId.equals(conversation.getBuyerId())
                    ? conversation.getSellerId()
                    : conversation.getBuyerId();
            messagingTemplate.convertAndSendToUser(
                    recipientId.toString(),
                    USER_MESSAGES_DEST,
                    ChatPushEvent.builder().type("new").message(message).build());
        } catch (Exception ex) {
            log.warn("Falha ao enviar mensagem em tempo real (conv={}): {}", conversation.getId(), ex.getMessage());
        }
    }
}
