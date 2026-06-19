package com.kumbu.backend.security;

import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.enums.AdminRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(User user, AdminRole adminRole) {
        this.id = user.getId();
        this.email = user.getEmail() != null ? user.getEmail() : user.getId().toString();
        this.passwordHash = user.getPasswordHash() != null ? user.getPasswordHash() : "";
        this.active = user.isActive();
        this.authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (adminRole != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + adminRole.name()));
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    public boolean isAdmin() {
        return authorities.stream().anyMatch(a -> {
            String role = a.getAuthority();
            return "ROLE_SUPER_ADMIN".equals(role)
                    || "ROLE_ADMIN".equals(role)
                    || "ROLE_SUPPORT".equals(role);
        });
    }
}
