package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.AppSupportSettings;
import com.kumbu.backend.domain.entity.ChatMessage;
import com.kumbu.backend.domain.entity.Conversation;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.enums.ConversationType;
import com.kumbu.backend.domain.enums.SupportStatus;
import com.kumbu.backend.dto.chat.ChatMessageResponse;
import com.kumbu.backend.dto.support.SupportConversationResponse;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.AppSupportSettingsRepository;
import com.kumbu.backend.repository.ChatMessageRepository;
import com.kumbu.backend.repository.ConversationRepository;
import com.kumbu.backend.repository.UserRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SupportService {

    public static final UUID SUPPORT_SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AppSupportSettingsRepository supportSettingsRepository;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;

    @Transactional
    public SupportConversationResponse getOrOpenConversation() {
        Conversation conversation = getOrCreateUserSupportConversation();
        return toConversationResponse(conversation, loadSettings());
    }

    @Transactional
    public List<ChatMessageResponse> listMessages() {
        Conversation conversation = getOrCreateUserSupportConversation();
        return chatMessageRepository
                .findByConversationIdAndHiddenAtIsNullOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(this::toMessage)
                .toList();
    }

    @Transactional
    public List<ChatMessageResponse> selectQuickAction(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            throw ApiException.badRequest("Opção inválida");
        }
        Conversation conversation = getOrCreateUserSupportConversation();
        if (conversation.getSupportStatus() == SupportStatus.CLOSED) {
            conversation.setSupportStatus(SupportStatus.BOT);
        }

        Map<String, Object> action = findQuickAction(actionId)
                .orElseThrow(() -> ApiException.badRequest("Opção de ajuda não encontrada"));

        String label = stringValue(action.get("label"));
        if (!label.isBlank()) {
            saveUserMessage(conversation, label);
        }

        boolean escalate = Boolean.TRUE.equals(action.get("escalate"));
        String answer = stringValue(action.get("answer"));

        List<ChatMessageResponse> responses = new ArrayList<>();
        if (!answer.isBlank()) {
            responses.add(saveSystemMessage(conversation.getId(), answer));
        }

        if (escalate) {
            escalateToAdmin(conversation);
            String auto = loadSettings().getAutoReplyMessage();
            if (!auto.isBlank()) {
                responses.add(saveSystemMessage(conversation.getId(), auto));
            }
        }

        touchConversation(conversation);
        return responses;
    }

    @Transactional
    public List<ChatMessageResponse> sendUserMessage(String body) {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest("Mensagem é obrigatória");
        }
        if (trimmed.length() > 4000) {
            throw ApiException.badRequest("Mensagem demasiado longa");
        }

        Conversation conversation = getOrCreateUserSupportConversation();
        if (conversation.getSupportStatus() == SupportStatus.CLOSED) {
            conversation.setSupportStatus(SupportStatus.BOT);
        }

        List<ChatMessageResponse> responses = new ArrayList<>();
        responses.add(saveUserMessage(conversation, trimmed));

        if (conversation.getSupportStatus() == SupportStatus.WAITING_ADMIN
                || conversation.getSupportStatus() == SupportStatus.ASSIGNED) {
            touchConversation(conversation);
            return responses;
        }

        Optional<String> faqAnswer = matchFaqAnswer(trimmed);
        if (faqAnswer.isPresent()) {
            responses.add(saveSystemMessage(conversation.getId(), faqAnswer.get()));
            touchConversation(conversation);
            return responses;
        }

        escalateToAdmin(conversation);
        String auto = loadSettings().getAutoReplyMessage();
        if (!auto.isBlank()) {
            responses.add(saveSystemMessage(conversation.getId(), auto));
        }
        touchConversation(conversation);
        return responses;
    }

    @Transactional(readOnly = true)
    public long countWaitingAdmin() {
        return conversationRepository.countByConversationTypeAndSupportStatus(
                ConversationType.SUPPORT, SupportStatus.WAITING_ADMIN);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listAdminConversations(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Conversation> result;
        if (status != null && !status.isBlank()) {
            SupportStatus supportStatus = parseSupportStatus(status);
            result = conversationRepository.findByConversationTypeAndSupportStatusOrderByUpdatedAtDesc(
                    ConversationType.SUPPORT, supportStatus, pageable);
        } else {
            result = conversationRepository.findByConversationTypeOrderByUpdatedAtDesc(
                    ConversationType.SUPPORT, pageable);
        }

        List<Map<String, Object>> items = result.getContent().stream()
                .map(this::toAdminConversationMap)
                .toList();

        return Map.of(
                "items", items,
                "page", result.getNumber(),
                "size", result.getSize(),
                "total", result.getTotalElements(),
                "total_pages", result.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAdminConversation(UUID conversationId) {
        Conversation conversation = requireSupportConversation(conversationId);
        Map<String, Object> map = toAdminConversationMap(conversation);
        List<Map<String, Object>> messages = chatMessageRepository
                .findByConversationIdAndHiddenAtIsNullOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .map(this::toAdminMessageMap)
                .toList();
        map.put("messages", messages);
        return map;
    }

    @Transactional
    public ChatMessageResponse adminReply(UUID conversationId, String body) {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty()) {
            throw ApiException.badRequest("Mensagem é obrigatória");
        }
        UUID adminId = securityUtils.currentUserId();
        Conversation conversation = requireSupportConversation(conversationId);
        if (conversation.getSupportStatus() == SupportStatus.CLOSED) {
            conversation.setSupportStatus(SupportStatus.ASSIGNED);
        } else if (conversation.getSupportStatus() == SupportStatus.WAITING_ADMIN) {
            conversation.setSupportStatus(SupportStatus.ASSIGNED);
        }

        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversation.getId())
                .senderId(SUPPORT_SYSTEM_USER_ID)
                .body(trimmed)
                .messageKind("support")
                .hiddenBy(adminId)
                .build());
        touchConversation(conversation);
        return toMessage(message);
    }

    @Transactional
    public Map<String, Object> adminCloseConversation(UUID conversationId) {
        Conversation conversation = requireSupportConversation(conversationId);
        conversation.setSupportStatus(SupportStatus.CLOSED);
        touchConversation(conversation);
        saveSystemMessage(conversation.getId(), "Esta conversa foi encerrada. Pode voltar a escrever para reabrir o suporte.");
        return Map.of("id", conversation.getId(), "support_status", "closed");
    }

    private Conversation getOrCreateUserSupportConversation() {
        UUID userId = securityUtils.currentUserId();
        Optional<Conversation> existing = conversationRepository
                .findByBuyerIdAndConversationType(userId, ConversationType.SUPPORT);
        if (existing.isPresent()) {
            return existing.get();
        }

        AppSupportSettings settings = loadSettings();
        try {
            Conversation created = conversationRepository.save(Conversation.builder()
                    .buyerId(userId)
                    .sellerId(SUPPORT_SYSTEM_USER_ID)
                    .conversationType(ConversationType.SUPPORT)
                    .supportStatus(SupportStatus.BOT)
                    .dealStatus(com.kumbu.backend.domain.enums.DealStatus.OPEN)
                    .build());
            saveSystemMessage(created.getId(), settings.getWelcomeMessage());
            return created;
        } catch (DataIntegrityViolationException ex) {
            return conversationRepository
                    .findByBuyerIdAndConversationType(userId, ConversationType.SUPPORT)
                    .orElseThrow(() -> ex);
        }
    }

    private Conversation requireSupportConversation(UUID conversationId) {
        return conversationRepository.findById(conversationId)
                .filter(c -> c.getConversationType() == ConversationType.SUPPORT)
                .orElseThrow(() -> ApiException.notFound("Conversa de suporte não encontrada"));
    }

    private void escalateToAdmin(Conversation conversation) {
        if (conversation.getSupportStatus() != SupportStatus.WAITING_ADMIN
                && conversation.getSupportStatus() != SupportStatus.ASSIGNED) {
            conversation.setSupportStatus(SupportStatus.WAITING_ADMIN);
            conversationRepository.save(conversation);
        }
    }

    private Optional<String> matchFaqAnswer(String userText) {
        String normalized = userText.toLowerCase(Locale.ROOT);
        for (Map<String, Object> action : loadSettings().getQuickActions()) {
            if (Boolean.TRUE.equals(action.get("escalate"))) {
                continue;
            }
            Object keywordsObj = action.get("keywords");
            if (!(keywordsObj instanceof List<?> keywords)) {
                continue;
            }
            for (Object keywordObj : keywords) {
                String keyword = stringValue(keywordObj);
                if (!keyword.isBlank() && normalized.contains(keyword.toLowerCase(Locale.ROOT))) {
                    String answer = stringValue(action.get("answer"));
                    if (!answer.isBlank()) {
                        return Optional.of(answer);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Map<String, Object>> findQuickAction(String actionId) {
        return loadSettings().getQuickActions().stream()
                .filter(action -> actionId.equals(stringValue(action.get("id"))))
                .findFirst();
    }

    private AppSupportSettings loadSettings() {
        return supportSettingsRepository.findById("default")
                .orElseThrow(() -> ApiException.notFound("Configuração de suporte em falta"));
    }

    private ChatMessageResponse saveUserMessage(Conversation conversation, String body) {
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversation.getId())
                .senderId(conversation.getBuyerId())
                .body(body)
                .messageKind("text")
                .build());
        return toMessage(message);
    }

    private ChatMessageResponse saveSystemMessage(UUID conversationId, String body) {
        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversationId)
                .senderId(SUPPORT_SYSTEM_USER_ID)
                .body(body)
                .messageKind("system")
                .build());
        return toMessage(message);
    }

    private void touchConversation(Conversation conversation) {
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);
    }

    private SupportConversationResponse toConversationResponse(
            Conversation conversation,
            AppSupportSettings settings) {
        return SupportConversationResponse.builder()
                .id(conversation.getId())
                .supportStatus(conversation.getSupportStatus() == null
                        ? "bot"
                        : conversation.getSupportStatus().name().toLowerCase())
                .welcomeMessage(settings.getWelcomeMessage())
                .quickActions(sanitizeQuickActions(settings.getQuickActions()))
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    private List<Map<String, Object>> sanitizeQuickActions(List<Map<String, Object>> actions) {
        if (actions == null) {
            return List.of();
        }
        return actions.stream().map(action -> {
            Map<String, Object> copy = new LinkedHashMap<>();
            copy.put("id", action.get("id"));
            copy.put("label", action.get("label"));
            if (action.get("escalate") != null) {
                copy.put("escalate", action.get("escalate"));
            }
            return copy;
        }).toList();
    }

    private Map<String, Object> toAdminConversationMap(Conversation conversation) {
        User user = userRepository.findById(conversation.getBuyerId()).orElse(null);
        ChatMessage last = chatMessageRepository
                .findTopByConversationIdAndHiddenAtIsNullOrderByCreatedAtDesc(conversation.getId())
                .orElse(null);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", conversation.getId());
        map.put("user_id", conversation.getBuyerId());
        map.put("user_name", user != null ? user.getDisplayName() : null);
        map.put("user_email", user != null ? user.getEmail() : null);
        map.put("support_status", conversation.getSupportStatus() == null
                ? "bot"
                : conversation.getSupportStatus().name().toLowerCase());
        map.put("updated_at", conversation.getUpdatedAt());
        map.put("last_message_body", last != null ? last.getBody() : null);
        map.put("last_message_at", last != null ? last.getCreatedAt() : null);
        return map;
    }

    private Map<String, Object> toAdminMessageMap(ChatMessage message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", message.getId());
        map.put("sender_id", message.getSenderId());
        map.put("body", message.getBody());
        map.put("message_kind", message.getMessageKind());
        map.put("created_at", message.getCreatedAt());
        map.put("from_support", isSupportSide(message));
        map.put("admin_actor_id", message.getHiddenBy());
        return map;
    }

    private ChatMessageResponse toMessage(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .body(message.getBody())
                .createdAt(message.getCreatedAt())
                .readAt(message.getReadAt())
                .messageKind(message.getMessageKind())
                .fromSupport(isSupportSide(message))
                .build();
    }

    private boolean isSupportSide(ChatMessage message) {
        return SUPPORT_SYSTEM_USER_ID.equals(message.getSenderId())
                || "system".equalsIgnoreCase(message.getMessageKind())
                || "support".equalsIgnoreCase(message.getMessageKind());
    }

    private static SupportStatus parseSupportStatus(String raw) {
        try {
            return SupportStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Estado de suporte inválido");
        }
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
