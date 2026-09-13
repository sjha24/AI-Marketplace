package com.aimarketplace.chat.pubsub;

import com.aimarketplace.chat.dto.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.chat", name = "redis-enabled", havingValue = "false", matchIfMissing = true)
public class LocalChatMessageFanout implements ChatMessageFanout {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(MessageResponse message) {
        messagingTemplate.convertAndSend(ChatMessageFanout.topicFor(message.orderId()), message);
    }
}
