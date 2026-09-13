package com.aimarketplace.chat.dto;

public record PresenceResponse(
        Long orderId,
        java.util.List<ConversationResponse.ParticipantPresence> participants
) {
}
