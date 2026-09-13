package com.aimarketplace.chat.presence;

import java.util.Collection;
import java.util.Map;

public interface PresenceTracker {
    void heartbeat(Long userId);

    void offline(Long userId);

    boolean isOnline(Long userId);

    Map<Long, Boolean> onlineMap(Collection<Long> userIds);
}
