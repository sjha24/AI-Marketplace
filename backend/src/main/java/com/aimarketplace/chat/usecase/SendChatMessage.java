package com.aimarketplace.chat.usecase;

import com.aimarketplace.chat.application.ChatApplicationService;
import com.aimarketplace.chat.dto.MessageResponse;
import com.aimarketplace.chat.dto.SendMessageRequest;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendChatMessage {
    private final ChatApplicationService service;

    @Logging
    public MessageResponse execute(Long orderId, SendMessageRequest request) {
        ValidationUtil.create()
                .require(orderId != null && orderId > 0, "orderId", "Order id is required")
                .require(request != null, "body", "Message body is required")
                .require(request != null && request.body() != null && !request.body().isBlank(),
                        "body", "Message body is required")
                .require(request == null || request.body() == null || request.body().length() <= 4000,
                        "body", "Message must be at most 4000 characters")
                .validate();
        return service.sendMessageForCurrentUser(orderId, request);
    }
}
