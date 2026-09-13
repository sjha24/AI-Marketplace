package com.aimarketplace.chat.dto;

public record WsChatSendPayload(
        Long orderId,
        String body
) {
}
