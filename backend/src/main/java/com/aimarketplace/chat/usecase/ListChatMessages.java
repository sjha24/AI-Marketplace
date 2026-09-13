package com.aimarketplace.chat.usecase;

import com.aimarketplace.chat.application.ChatApplicationService;
import com.aimarketplace.chat.dto.MessageResponse;
import com.aimarketplace.job.dto.PageResponse;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListChatMessages {
    private final ChatApplicationService service;

    @Logging
    public PageResponse<MessageResponse> execute(Long orderId, int page, int size) {
        ValidationUtil.create()
                .require(orderId != null && orderId > 0, "orderId", "Order id is required")
                .validate();
        return service.listMessages(orderId, page, size);
    }
}
