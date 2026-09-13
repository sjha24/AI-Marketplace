package com.aimarketplace.chat.usecase;

import com.aimarketplace.chat.application.ChatApplicationService;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkConversationRead {
    private final ChatApplicationService service;

    @Logging
    public void execute(Long orderId) {
        ValidationUtil.create()
                .require(orderId != null && orderId > 0, "orderId", "Order id is required")
                .validate();
        service.markConversationRead(orderId);
    }
}
