package com.kumbu.backend.service;

import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.dto.order.CartItemRequest;
import com.kumbu.backend.dto.user.DeliveryAddressRequest;
import com.kumbu.backend.dto.user.UserPublicProfileResponse;
import com.kumbu.backend.dto.user.UpdateProfileRequest;
import com.kumbu.backend.dto.user.UserProfileResponse;
import com.kumbu.backend.exception.ApiException;
import com.kumbu.backend.repository.UserRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final ProfileCompletenessService profileCompletenessService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse me() {
        return toProfile(getCurrentUser());
    }

    @Transactional(readOnly = true)
    public UserPublicProfileResponse getPublicProfile(UUID userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> ApiException.notFound("Utilizador não encontrado"));
        if (user.isBanned()) {
            throw ApiException.notFound("Utilizador não encontrado");
        }
        return toPublicProfile(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> publishReadiness() {
        return profileCompletenessService.assess(getCurrentUser());
    }

    @Transactional
    public UserProfileResponse updateProfile(UpdateProfileRequest request) {
        if (request.getFullName() == null && request.getPhone() == null
                && request.getCity() == null && request.getRegion() == null
                && request.getCountry() == null && request.getPhotoUrl() == null
                && request.getGender() == null && request.getBirthDate() == null) {
            throw ApiException.badRequest("Indique pelo menos um campo para actualizar");
        }
        User user = getCurrentUser();
        if (request.getFullName() != null) {
            if (request.getFullName().isBlank()) {
                throw ApiException.badRequest("Nome completo não pode estar vazio");
            }
            user.setDisplayName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().isBlank() ? null : request.getPhone().trim());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity().isBlank() ? null : request.getCity().trim());
        }
        if (request.getRegion() != null) {
            user.setRegion(request.getRegion().isBlank() ? null : request.getRegion().trim());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry().isBlank() ? null : request.getCountry().trim());
        }
        if (request.getPhotoUrl() != null) {
            user.setPhotoUrl(request.getPhotoUrl().isBlank() ? null : request.getPhotoUrl().trim());
        }
        if (request.getGender() != null) {
            if (request.getGender().isBlank()) {
                user.setGender(null);
            } else {
                try {
                    user.setGender(ProfileCompletenessService.normalizeGender(request.getGender()));
                } catch (IllegalArgumentException ex) {
                    throw ApiException.badRequest(ex.getMessage());
                }
            }
        }
        if (request.getBirthDate() != null) {
            try {
                ProfileCompletenessService.validateBirthDate(request.getBirthDate());
            } catch (IllegalArgumentException ex) {
                throw ApiException.badRequest(ex.getMessage());
            }
            user.setBirthDate(request.getBirthDate());
        }
        return toProfile(userRepository.save(user));
    }

    @Transactional
    public void addFavorite(String productId) {
        User user = getCurrentUser();
        if (user.getFavorites() == null) {
            user.setFavorites(new ArrayList<>());
        }
        if (!user.getFavorites().contains(productId)) {
            user.getFavorites().add(productId);
            userRepository.save(user);
        }
    }

    @Transactional
    public void removeFavorite(String productId) {
        User user = getCurrentUser();
        if (user.getFavorites() != null) {
            user.getFavorites().remove(productId);
            userRepository.save(user);
        }
    }

    @Transactional
    public UserProfileResponse updateDeliveryAddress(DeliveryAddressRequest address) {
        User user = getCurrentUser();
        user.setDeliveryAddress(toDeliveryAddressMap(address));
        if (address.getCity() != null && !address.getCity().isBlank()) {
            user.setCity(address.getCity().trim());
        }
        if (address.getRegion() != null && !address.getRegion().isBlank()) {
            user.setRegion(address.getRegion().trim());
        }
        if (address.getCountry() != null && !address.getCountry().isBlank()) {
            user.setCountry(address.getCountry().trim());
        }
        return toProfile(userRepository.save(user));
    }

    private Map<String, String> toDeliveryAddressMap(DeliveryAddressRequest address) {
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
        return map;
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        User user = getCurrentUser();
        if (user.getPasswordHash() == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("Palavra-passe actual incorrecta");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw ApiException.badRequest("A nova palavra-passe deve ter pelo menos 8 caracteres");
        }
        user.setPasswordHash(authService.encodePassword(newPassword));
        authService.invalidateAllSessions(user);
    }

    @Transactional
    public void syncCart(java.util.List<CartItemRequest> cartItems) {
        User user = getCurrentUser();
        user.setCart(new ArrayList<>(cartItems));
        userRepository.save(user);
    }

    @Transactional
    public void deleteAccount() {
        User user = getCurrentUser();
        user.setDeletedAt(Instant.now());
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportAccountData() {
        User user = getCurrentUser();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", toProfile(user));
        data.put("favorites", user.getFavorites());
        data.put("cart", user.getCart());
        data.put("exportedAt", Instant.now().toString());
        return data;
    }

    private User getCurrentUser() {
        UUID id = securityUtils.currentUserId();
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Utilizador não encontrado"));
    }

    private UserProfileResponse toProfile(User user) {
        Map<String, Object> readiness = profileCompletenessService.assess(user);
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) readiness.get("missing_fields");
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getDisplayName())
                .phone(user.getPhone())
                .profileImageUrl(user.getPhotoUrl())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .sellerVerified(user.isSellerVerified())
                .favorites(user.getFavorites())
                .deliveryAddress(user.getDeliveryAddress())
                .city(user.getCity())
                .region(user.getRegion())
                .country(user.getCountry())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .age(ProfileCompletenessService.computeAge(user.getBirthDate()))
                .profileComplete((Boolean) readiness.get("profile_complete"))
                .canPublish((Boolean) readiness.get("can_publish"))
                .missingProfileFields(missing)
                .cart(user.getCart())
                .bannedAt(user.getBannedAt())
                .bannedUntil(user.getBannedUntil())
                .banReason(user.getBanReason())
                .accountSuspended(user.isBanned())
                .build();
    }

    private UserPublicProfileResponse toPublicProfile(User user) {
        return UserPublicProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getDisplayName())
                .profileImageUrl(user.getPhotoUrl())
                .sellerVerified(user.isSellerVerified())
                .city(user.getCity())
                .region(user.getRegion())
                .country(user.getCountry())
                .build();
    }
}
