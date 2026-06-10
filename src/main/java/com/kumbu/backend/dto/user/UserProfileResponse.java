package com.kumbu.backend.dto.user;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserProfileResponse {
    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private String profileImageUrl;
    private boolean emailVerified;
    private boolean phoneVerified;
    private List<String> favorites;
    private Object deliveryAddress;
    private String city;
    private String region;
    private String country;
    private String gender;
    private LocalDate birthDate;
    private Integer age;
    private boolean profileComplete;
    private boolean canPublish;
    private List<String> missingProfileFields;
    private java.util.List<Object> cart;
}
