package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.*;
import com.kumbu.backend.domain.enums.DealStatus;
import com.kumbu.backend.domain.enums.OrderStatus;
import com.kumbu.backend.dto.order.CartItemRequest;
import com.kumbu.backend.dto.order.OrderResponse;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.*;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CatalogProductRepository productRepository;
    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<OrderResponse> listPurchases() {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(securityUtils.currentUserId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listSales() {
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(securityUtils.currentUserId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        UUID userId = securityUtils.currentUserId();
        Order order = orderRepository.findById(orderId)
                .filter(o -> userId.equals(o.getUserId()) || userId.equals(o.getSellerId()))
                .orElseThrow(() -> ApiException.notFound("Encomenda não encontrada"));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(String orderId, String nextStatus) {
        UUID sellerId = securityUtils.currentUserId();
        Order order = orderRepository.findByIdAndSellerId(orderId, sellerId)
                .orElseThrow(() -> ApiException.notFound("Encomenda não encontrada"));

        OrderStatus current = order.getStatus();
        OrderStatus next = OrderStatus.valueOf(nextStatus.toUpperCase());
        validateTransition(current, next);

        order.setStatus(next);
        return toResponse(orderRepository.save(order));
    }

    @Transactional
    public CheckoutResult checkout(List<CartItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw ApiException.badRequest("Carrinho vazio");
        }

        UUID buyerId = securityUtils.currentUserId();
        Map<UUID, List<CartItemRequest>> bySeller = items.stream()
                .collect(Collectors.groupingBy(i -> UUID.fromString(i.getSellerId())));

        List<OrderResponse> created = new ArrayList<>();
        List<String> conversationIds = new ArrayList<>();

        for (Map.Entry<UUID, List<CartItemRequest>> entry : bySeller.entrySet()) {
            UUID sellerId = entry.getKey();
            List<CartItemRequest> sellerItems = entry.getValue();

            int count = sellerItems.stream().mapToInt(CartItemRequest::getQuantity).sum();
            String total = computeTotal(sellerItems);
            String orderId = "ord_" + System.currentTimeMillis() + "_" + sellerId.toString().substring(0, 8);

            Order order = Order.builder()
                    .id(orderId)
                    .userId(buyerId)
                    .sellerId(sellerId)
                    .itemsCount(count)
                    .totalLabel(total)
                    .status(OrderStatus.PROCESSING)
                    .build();
            orderRepository.save(order);

            for (CartItemRequest item : sellerItems) {
                orderItemRepository.save(OrderItem.builder()
                        .orderId(orderId)
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .priceLabel(item.getPriceLabel())
                        .title(item.getTitle())
                        .build());

                String convId = openConversationWithOrderMessage(buyerId, sellerId, item.getProductId(), order);
                conversationIds.add(convId);
            }

            created.add(toResponse(order));
            notifySellerNewOrder(sellerId, order);
        }

        return new CheckoutResult(created, conversationIds);
    }

    private String openConversationWithOrderMessage(UUID buyerId, UUID sellerId, String productId, Order order) {
        Conversation conversation = conversationRepository
                .findByProductIdAndBuyerId(productId, buyerId)
                .orElseGet(() -> conversationRepository.save(Conversation.builder()
                        .productId(productId)
                        .buyerId(buyerId)
                        .sellerId(sellerId)
                        .dealStatus(DealStatus.OPEN)
                        .build()));

        chatMessageRepository.save(ChatMessage.builder()
                .conversationId(conversation.getId())
                .senderId(buyerId)
                .body("Nova encomenda " + order.getId() + " — " + order.getTotalLabel())
                .messageKind("order")
                .build());
        return conversation.getId().toString();
    }

    private void notifySellerNewOrder(UUID sellerId, Order order) {
        notificationService.saveAndPush(UserNotification.builder()
                .userId(sellerId)
                .title("Nova encomenda")
                .body("Encomenda " + order.getId() + " — " + order.getTotalLabel())
                .iconKey("shopping_bag_outlined")
                .actionUrl("/conta/vendas")
                .build());
    }

    private void validateTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PROCESSING -> next == OrderStatus.SHIPPING || next == OrderStatus.DELIVERED || next == OrderStatus.CANCELLED;
            case SHIPPING -> next == OrderStatus.DELIVERED || next == OrderStatus.CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!valid) {
            throw ApiException.badRequest("Transição de estado inválida: " + current + " → " + next);
        }
    }

    private String computeTotal(List<CartItemRequest> items) {
        long total = items.stream()
                .mapToLong(i -> CatalogService.parsePrice(i.getPriceLabel()).longValue() * i.getQuantity())
                .sum();
        return String.format("%,d Kz".replace(',', '.'), total).replace('.', '.');
    }

    private OrderResponse toResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .sellerId(order.getSellerId())
                .status(order.getStatus().name().toLowerCase())
                .totalLabel(order.getTotalLabel())
                .itemsCount(order.getItemsCount())
                .createdAt(order.getCreatedAt())
                .showTrack(order.isShowTrack())
                .build();
    }

    public record CheckoutResult(List<OrderResponse> orders, List<String> conversationIds) {}
}
