package com.edu.registration.security;

import com.edu.registration.entity.Role;
import com.edu.registration.entity.User;
import com.edu.registration.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserAuthCache {

    private static final long TTL_MS = 30_000;

    private final UserRepository userRepository;
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public UserAuthCache(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<AuthUser> find(String username) {
        long now = System.currentTimeMillis();
        Entry entry = cache.get(username);
        if (entry != null && entry.expiresAt() > now) {
            return Optional.of(entry.user());
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            cache.remove(username);
            return Optional.empty();
        }

        AuthUser authUser = new AuthUser(user.getUsername(), user.getRole(), user.isTrangThai(),
                user.getCurrentSessionId());
        cache.put(username, new Entry(authUser, now + TTL_MS));
        return Optional.of(authUser);
    }

    public void evict(String username) {
        if (username != null) {
            cache.remove(username);
        }
    }

    public void evictAll() {
        cache.clear();
    }

    public record AuthUser(String username, Role role, boolean active, String sessionId) {
    }

    private record Entry(AuthUser user, long expiresAt) {
    }
}
