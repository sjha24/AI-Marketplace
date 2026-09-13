package com.aimarketplace.chat.usecase;

import com.aimarketplace.chat.application.ChatApplicationService;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkMessageRead {
    private final ChatApplicationService service;

    @Logging
    public void execute(Long messageId) {
        ValidationUtil.create()
                .require(messageId != null && messageId > 0, "id", "Message id is required")
                .validate();
        service.markRead(messageId);
    }
}
