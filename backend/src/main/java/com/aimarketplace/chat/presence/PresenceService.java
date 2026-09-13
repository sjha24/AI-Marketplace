package com.aimarketplace.chat.presence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PresenceService {
    private final PresenceTracker tracker;

    public void heartbeat(Long userId) {
        tracker.heartbeat(userId);
    }

    public void offline(Long userId) {
        tracker.offline(userId);
    }

    public boolean isOnline(Long userId) {
        return tracker.isOnline(userId);
    }

    public Map<Long, Boolean> onlineMap(Collection<Long> userIds) {
        return tracker.onlineMap(userIds);
    }
}
