package com.kumbu.backend.security;

import com.kumbu.backend.domain.entity.AdminUser;
import com.kumbu.backend.domain.entity.User;
import com.kumbu.backend.domain.enums.AdminRole;
import com.kumbu.backend.repository.AdminUserRepository;
import com.kumbu.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = header.substring(7);
            UUID userId = jwtService.extractUserId(token);
            int tokenVersion = jwtService.extractTokenVersion(token);

            User user = userRepository.findById(userId).orElse(null);
            if (user != null
                    && user.isActive()
                    && user.getTokenVersion() == tokenVersion
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                AdminRole role = adminUserRepository.findByUserId(userId)
                        .map(AdminUser::getRole)
                        .orElse(null);
                UserPrincipal principal = new UserPrincipal(user, role);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception ignored) {
            // Token inválido — continua sem autenticação
        }

        filterChain.doFilter(request, response);
    }
}
