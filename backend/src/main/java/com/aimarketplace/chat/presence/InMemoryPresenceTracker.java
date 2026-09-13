package com.aimarketplace.chat.presence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(prefix = "app.chat", name = "redis-enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryPresenceTracker implements PresenceTracker {
    private static final Duration TTL = Duration.ofSeconds(45);
    private final ConcurrentHashMap<Long, Instant> onlineUntil = new ConcurrentHashMap<>();

    @Override
    public void heartbeat(Long userId) {
        if (userId == null) {
            return;
        }
        onlineUntil.put(userId, Instant.now().plus(TTL));
    }

    @Override
    public void offline(Long userId) {
        if (userId == null) {
            return;
        }
        onlineUntil.remove(userId);
    }

    @Override
    public boolean isOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        Instant until = onlineUntil.get(userId);
        if (until == null) {
            return false;
        }
        if (until.isBefore(Instant.now())) {
            onlineUntil.remove(userId, until);
            return false;
        }
        return true;
    }

    @Override
    public Map<Long, Boolean> onlineMap(Collection<Long> userIds) {
        Map<Long, Boolean> result = new HashMap<>();
        for (Long userId : userIds) {
            result.put(userId, isOnline(userId));
        }
        return result;
    }
}
