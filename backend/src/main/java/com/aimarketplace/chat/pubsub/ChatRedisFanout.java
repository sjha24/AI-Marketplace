package com.aimarketplace.chat.pubsub;

import com.aimarketplace.chat.dto.MessageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.chat", name = "redis-enabled", havingValue = "true")
public class ChatRedisFanout implements ChatMessageFanout, MessageListener {
    private static final Logger log = LoggerFactory.getLogger(ChatRedisFanout.class);
    public static final String CHANNEL = "chat:messages";

    private final StringRedisTemplate redis;
    private final RedisMessageListenerContainer listenerContainer;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @PostConstruct
    void subscribe() {
        listenerContainer.addMessageListener(this, new ChannelTopic(CHANNEL));
    }

    @Override
    public void publish(MessageResponse message) {
        try {
            redis.convertAndSend(CHANNEL, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            log.warn("Failed to publish chat message to Redis: {}", e.getMessage());
            messagingTemplate.convertAndSend(ChatMessageFanout.topicFor(message.orderId()), message);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            MessageResponse payload = objectMapper.readValue(message.getBody(), MessageResponse.class);
            messagingTemplate.convertAndSend(ChatMessageFanout.topicFor(payload.orderId()), payload);
        } catch (Exception e) {
            log.warn("Failed to fan-out chat message from Redis: {}", e.getMessage());
        }
    }
}
