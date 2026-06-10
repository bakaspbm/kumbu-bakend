package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.AppPaymentMethod;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.dto.order.CartItemRequest;
import com.kumbu.backend.dto.user.DeliveryAddressRequest;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.AppPaymentMethodRepository;
import com.kumbu.backend.repository.UserRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final UserRepository userRepository;
    private final AppPaymentMethodRepository paymentMethodRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public StoreUserDto getStoreUser() {
        User user = userRepository.findById(securityUtils.currentUserId())
                .orElseThrow(() -> ApiException.notFound("Utilizador não encontrado"));
        return toStoreUser(user);
    }

    @Transactional(readOnly = true)
    public List<PaymentMethodDto> listPaymentMethods() {
        return paymentMethodRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toPaymentMethod)
                .toList();
    }

    @Transactional
    public StoreUserDto updateDeliveryAddress(DeliveryAddressRequest address) {
        User user = userRepository.findById(securityUtils.currentUserId()).orElseThrow();
        Map<String, String> map = new LinkedHashMap<>();
        map.put("recipientName", address.getRecipientName());
        map.put("phone", address.getPhone());
        map.put("street", address.getStreet());
        if (address.getCity() != null && !address.getCity().isBlank()) {
            map.put("city", address.getCity().trim());
        }
        if (address.getRegion() != null && !address.getRegion().isBlank()) {
            map.put("region", address.getRegion().trim());
        }
        if (address.getCountry() != null && !address.getCountry().isBlank()) {
            map.put("country", address.getCountry().trim());
        }
        if (address.getPostalCode() != null && !address.getPostalCode().isBlank()) {
            map.put("postalCode", address.getPostalCode().trim());
        }
        user.setDeliveryAddress(map);
        return toStoreUser(userRepository.save(user));
    }

    @Transactional
    public void syncCart(List<CartItemRequest> items) {
        User user = userRepository.findById(securityUtils.currentUserId()).orElseThrow();
        user.setCart(new ArrayList<>(items));
        userRepository.save(user);
    }

    private StoreUserDto toStoreUser(User user) {
        return StoreUserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .phone(user.getPhone())
                .photoUrl(user.getPhotoUrl())
                .favorites(user.getFavorites())
                .cart(user.getCart())
                .deliveryAddress(user.getDeliveryAddress())
                .build();
    }

    private PaymentMethodDto toPaymentMethod(AppPaymentMethod m) {
        return PaymentMethodDto.builder()
                .id(m.getId())
                .label(m.getLabel())
                .iconKey(m.getIconKey())
                .isDefault(m.isDefault())
                .sortOrder(m.getSortOrder())
                .build();
    }

    @Data @Builder public static class StoreUserDto {
        private UUID id; private String email; private String displayName; private String phone;
        private String photoUrl; private List<String> favorites; private List<Object> cart;
        private Object deliveryAddress;
    }

    @Data @Builder public static class PaymentMethodDto {
        private String id; private String label; private String iconKey; private boolean isDefault; private int sortOrder;
    }
}
