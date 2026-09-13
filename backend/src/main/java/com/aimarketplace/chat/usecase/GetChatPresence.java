package com.aimarketplace.chat.usecase;

import com.aimarketplace.chat.application.ChatApplicationService;
import com.aimarketplace.chat.dto.PresenceResponse;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetChatPresence {
    private final ChatApplicationService service;

    @Logging
    public PresenceResponse execute(Long orderId) {
        ValidationUtil.create()
                .require(orderId != null && orderId > 0, "orderId", "Order id is required")
                .validate();
        return service.presence(orderId);
    }
}
