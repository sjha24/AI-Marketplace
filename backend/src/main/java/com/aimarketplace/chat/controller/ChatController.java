package com.aimarketplace.chat.controller;

import com.aimarketplace.chat.dto.ConversationResponse;
import com.aimarketplace.chat.dto.MessageResponse;
import com.aimarketplace.chat.dto.PresenceResponse;
import com.aimarketplace.chat.dto.SendMessageRequest;
import com.aimarketplace.chat.usecase.*;
import com.aimarketplace.job.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final GetConversation getConversation;
    private final ListChatMessages listChatMessages;
    private final SendChatMessage sendChatMessage;
    private final MarkMessageRead markMessageRead;
    private final MarkConversationRead markConversationRead;
    private final GetChatPresence getChatPresence;

    @GetMapping("/orders/{orderId}")
    public ConversationResponse conversation(@PathVariable Long orderId) {
        return getConversation.execute(orderId);
    }

    @GetMapping("/orders/{orderId}/messages")
    public PageResponse<MessageResponse> messages(
            @PathVariable Long orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return listChatMessages.execute(orderId, page, size);
    }

    @PostMapping("/orders/{orderId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(
            @PathVariable Long orderId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return sendChatMessage.execute(orderId, request);
    }

    @PostMapping("/messages/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@PathVariable Long id) {
        markMessageRead.execute(id);
    }

    @PostMapping("/orders/{orderId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markOrderRead(@PathVariable Long orderId) {
        markConversationRead.execute(orderId);
    }

    @GetMapping("/orders/{orderId}/presence")
    public PresenceResponse presence(@PathVariable Long orderId) {
        return getChatPresence.execute(orderId);
    }
}
