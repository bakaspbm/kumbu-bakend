package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.CatalogProduct;
import com.kumbu.backend.domain.entity.PropertyRentalRequest;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.CatalogProductRepository;
import com.kumbu.backend.repository.PropertyRentalRequestRepository;
import com.kumbu.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRentalsService {

    private final PropertyRentalRequestRepository rentalRepository;
    private final CatalogProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> listRentals(String status, int page, int size) {
        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));
        Page<PropertyRentalRequest> result = blankToNull(status) == null
                ? rentalRepository.findAllByOrderByCreatedAtDesc(pageable)
                : rentalRepository.findByStatusOrderByCreatedAtDesc(status.trim().toLowerCase(), pageable);
        List<Map<String, Object>> items = result.getContent().stream()
                .map(this::toRentalMap)
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
    public Map<String, Object> getRental(UUID id) {
        PropertyRentalRequest rental = rentalRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Reserva não encontrada"));
        return toRentalDetailMap(rental);
    }

    @Transactional(readOnly = true)
    public long countPendingRentals() {
        return rentalRepository.countByStatus("pending");
    }

    private Map<String, Object> toRentalMap(PropertyRentalRequest rental) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", rental.getId());
        map.put("product_id", rental.getProductId());
        map.put("product_title", resolveProductTitle(rental.getProductId()));
        map.put("renter_id", rental.getRenterId());
        map.put("owner_id", rental.getOwnerId());
        map.put("renter_name", userLabel(rental.getRenterId()));
        map.put("owner_name", userLabel(rental.getOwnerId()));
        map.put("rental_mode", rental.getRentalMode());
        map.put("check_in", rental.getCheckIn());
        map.put("check_out", rental.getCheckOut());
        map.put("nights", rental.getNights());
        map.put("status", rental.getStatus());
        map.put("price_snapshot", rental.getPriceSnapshot());
        map.put("created_at", rental.getCreatedAt());
        return map;
    }

    private Map<String, Object> toRentalDetailMap(PropertyRentalRequest rental) {
        Map<String, Object> map = toRentalMap(rental);
        map.put("guest_message", rental.getGuestMessage());
        map.put("conversation_id", rental.getConversationId());
        map.put("updated_at", rental.getUpdatedAt());
        map.put("renter_email", userEmail(rental.getRenterId()));
        map.put("owner_email", userEmail(rental.getOwnerId()));
        return map;
    }

    private String resolveProductTitle(String productId) {
        return productRepository.findById(productId).map(CatalogProduct::getTitle).orElse(productId);
    }

    private String userLabel(UUID userId) {
        return userRepository.findById(userId)
                .map(u -> u.getDisplayName() != null ? u.getDisplayName() : u.getEmail())
                .orElse(userId.toString().substring(0, 8));
    }

    private String userEmail(UUID userId) {
        return userRepository.findById(userId).map(User::getEmail).orElse(null);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
