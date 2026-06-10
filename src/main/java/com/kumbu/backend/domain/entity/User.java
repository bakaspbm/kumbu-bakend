package com.kumbu.backend.domain.entity;

import com.kumbu.backend.domain.enums.SignupAuthMethod;
import com.kumbu.backend.domain.enums.SignupSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "display_name")
    private String displayName;

    private String phone;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<Object> cart = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private List<String> favorites = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "delivery_address", columnDefinition = "jsonb")
    private Map<String, String> deliveryAddress;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    private String city;
    private String region;
    private String country;

    @Column(name = "signup_source", nullable = false)
    @Builder.Default
    private SignupSource signupSource = SignupSource.UNKNOWN;

    @Column(name = "signup_auth_method", nullable = false)
    @Builder.Default
    private SignupAuthMethod signupAuthMethod = SignupAuthMethod.EMAIL;

    @Column(name = "last_active_source", nullable = false)
    @Builder.Default
    private SignupSource lastActiveSource = SignupSource.UNKNOWN;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "phone_verified", nullable = false)
    @Builder.Default
    private boolean phoneVerified = false;

    @Column(name = "banned_at")
    private Instant bannedAt;

    @Column(name = "banned_until")
    private Instant bannedUntil;

    @Column(name = "ban_reason")
    private String banReason;

    @Column(name = "banned_by")
    private UUID bannedBy;

    @Column(name = "seller_verified", nullable = false)
    @Builder.Default
    private boolean sellerVerified = false;

    @Column(name = "seller_verification_tier")
    private String sellerVerificationTier;

    @Column(name = "vip_until")
    private Instant vipUntil;

    @Column(name = "business_plan_id")
    private String businessPlanId;

    @Column(name = "max_listings", nullable = false)
    @Builder.Default
    private int maxListings = 3;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isBanned() {
        if (bannedAt == null || deletedAt != null) return false;
        return bannedUntil == null || bannedUntil.isAfter(Instant.now());
    }

    public boolean isActive() {
        return deletedAt == null && !isBanned();
    }

    public boolean isVipActive() {
        return vipUntil != null && vipUntil.isAfter(Instant.now());
    }
}
