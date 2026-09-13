package com.aimarketplace.chat.dto;

import java.time.Instant;
import java.util.List;

public record ConversationResponse(
        Long id,
        Long orderId,
        Instant createdAt,
        long unreadCount,
        List<ParticipantPresence> participants
) {
    public record ParticipantPresence(
            Long userId,
            String displayName,
            boolean online
    ) {
    }
}
