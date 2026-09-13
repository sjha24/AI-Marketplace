package com.aimarketplace.chat.dto;

import com.aimarketplace.chat.domain.model.MessageType;

import java.time.Instant;

public record MessageResponse(
        Long id,
        Long conversationId,
        Long orderId,
        Long senderId,
        String senderDisplayName,
        MessageType messageType,
        String body,
        String attachmentUrl,
        Instant createdAt,
        boolean mine,
        boolean readByMe
) {
}
