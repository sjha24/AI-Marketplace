package com.aimarketplace.chat.websocket;

import com.aimarketplace.chat.application.ChatApplicationService;
import com.aimarketplace.chat.presence.PresenceService;
import com.aimarketplace.shared.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ChatWsEventListener {
    private static final Pattern ORDER_TOPIC = Pattern.compile("^/topic/orders\\.(\\d+)\\.chat$");

    private final PresenceService presenceService;
    private final ChatApplicationService chatApplicationService;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Long userId = resolveUserId(event.getUser());
        if (userId != null) {
            presenceService.heartbeat(userId);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Long userId = resolveUserId(event.getUser());
        if (userId != null) {
            presenceService.offline(userId);
        }
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }
        Matcher matcher = ORDER_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            return;
        }
        Long orderId = Long.valueOf(matcher.group(1));
        Long userId = resolveUserId(event.getUser());
        if (userId == null) {
            throw new IllegalArgumentException("Unauthenticated subscription");
        }
        chatApplicationService.assertCanSubscribe(orderId, userId);
        presenceService.heartbeat(userId);
    }

    private Long resolveUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof UserPrincipal user) {
            return user.id();
        }
        return null;
    }
}
