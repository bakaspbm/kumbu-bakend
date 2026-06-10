package com.kumbu.backend.service;

import com.kumbu.backend.repository.UserRepository;
import com.kumbu.backend.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class UserPresenceService {

    public static final Duration ONLINE_WINDOW = Duration.ofMinutes(5);

    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final ConcurrentHashMap<UUID, Long> lastTouchMs = new ConcurrentHashMap<>();

    public boolean isOnline(Instant lastSeenAt) {
        if (lastSeenAt == null) {
            return false;
        }
        return lastSeenAt.isAfter(Instant.now().minus(ONLINE_WINDOW));
    }

    @Transactional
    public void touchCurrentUser() {
        touch(securityUtils.currentUserId());
    }

    @Transactional
    public void touch(UUID userId) {
        if (userId == null) {
            return;
        }
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastSeenAt(Instant.now());
            userRepository.save(user);
        });
    }

    /** Evita escritas excessivas — no máximo 1 update/minuto por utilizador. */
    public void touchIfDue(UUID userId) {
        if (userId == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = lastTouchMs.get(userId);
        if (previous != null && now - previous < 60_000) {
            return;
        }
        lastTouchMs.put(userId, now);
        touch(userId);
    }
}
