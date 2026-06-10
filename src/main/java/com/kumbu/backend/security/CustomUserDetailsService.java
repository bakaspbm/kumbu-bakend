package com.kumbu.backend.security;

import com.kumbu.backend.domain.entity.AdminUser;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.repository.AdminUserRepository;
import com.kumbu.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado"));
        AdminUser admin = adminUserRepository.findByUserId(user.getId()).orElse(null);
        return new UserPrincipal(user, admin != null ? admin.getRole() : null);
    }
}
