package com.aimarketplace.chat.dto;

import com.aimarketplace.chat.domain.model.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank @Size(max = 4000) String body
) {
    public MessageType type() {
        return MessageType.TEXT;
    }
}
