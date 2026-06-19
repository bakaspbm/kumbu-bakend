package com.kumbu.backend.dto.user;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserPublicProfileResponse {
    private UUID id;
    private String fullName;
    private String profileImageUrl;
    private boolean sellerVerified;
    private String city;
    private String region;
    private String country;
}
