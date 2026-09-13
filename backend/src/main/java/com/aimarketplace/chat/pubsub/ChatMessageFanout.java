package com.aimarketplace.chat.pubsub;

import com.aimarketplace.chat.dto.MessageResponse;

public interface ChatMessageFanout {
    void publish(MessageResponse message);

    static String topicFor(Long orderId) {
        return "/topic/orders." + orderId + ".chat";
    }
}
