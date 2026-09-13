package com.aimarketplace.chat.presence;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "app.chat", name = "redis-enabled", havingValue = "true")
public class RedisPresenceTracker implements PresenceTracker {
    private static final String KEY_PREFIX = "presence:";
    private static final Duration TTL = Duration.ofSeconds(45);

    private final StringRedisTemplate redis;

    public RedisPresenceTracker(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void heartbeat(Long userId) {
        if (userId == null) {
            return;
        }
        redis.opsForValue().set(KEY_PREFIX + userId, "1", TTL);
    }

    @Override
    public void offline(Long userId) {
        if (userId == null) {
            return;
        }
        redis.delete(KEY_PREFIX + userId);
    }

    @Override
    public boolean isOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + userId));
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
