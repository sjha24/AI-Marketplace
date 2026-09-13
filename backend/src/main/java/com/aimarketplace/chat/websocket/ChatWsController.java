package com.aimarketplace.chat.websocket;

import com.aimarketplace.chat.application.ChatApplicationService;
import com.aimarketplace.chat.dto.SendMessageRequest;
import com.aimarketplace.chat.dto.WsChatSendPayload;
import com.aimarketplace.chat.presence.PresenceService;
import com.aimarketplace.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatApplicationService chatApplicationService;
    private final PresenceService presenceService;

    @MessageMapping("chat.send")
    public void send(@Payload WsChatSendPayload payload, Principal principal) {
        UserPrincipal user = requireUser(principal);
        presenceService.heartbeat(user.id());
        chatApplicationService.sendMessage(
                payload.orderId(),
                user.id(),
                new SendMessageRequest(payload.body())
        );
    }

    @MessageMapping("presence.heartbeat")
    public void heartbeat(Principal principal) {
        UserPrincipal user = requireUser(principal);
        presenceService.heartbeat(user.id());
    }

    private UserPrincipal requireUser(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof UserPrincipal user) {
            return user;
        }
        throw new IllegalArgumentException("Unauthenticated WebSocket user");
    }
}
