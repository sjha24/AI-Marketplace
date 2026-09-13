package com.aimarketplace.chat.application;

import com.aimarketplace.chat.domain.model.MessageType;
import com.aimarketplace.chat.dto.ConversationResponse;
import com.aimarketplace.chat.dto.MessageResponse;
import com.aimarketplace.chat.dto.PresenceResponse;
import com.aimarketplace.chat.dto.SendMessageRequest;
import com.aimarketplace.chat.entity.ChatMessage;
import com.aimarketplace.chat.entity.Conversation;
import com.aimarketplace.chat.entity.ConversationParticipant;
import com.aimarketplace.chat.entity.MessageRead;
import com.aimarketplace.chat.mapper.ChatMessageMapper;
import com.aimarketplace.chat.presence.PresenceService;
import com.aimarketplace.chat.pubsub.ChatMessageFanout;
import com.aimarketplace.chat.repository.ChatMessageRepository;
import com.aimarketplace.chat.repository.ConversationParticipantRepository;
import com.aimarketplace.chat.repository.ConversationRepository;
import com.aimarketplace.chat.repository.MessageReadRepository;
import com.aimarketplace.job.dto.PageResponse;
import com.aimarketplace.order.domain.model.OrderStatus;
import com.aimarketplace.order.entity.Order;
import com.aimarketplace.order.repository.OrderRepository;
import com.aimarketplace.payment.event.OrderPaidEscrowEvent;
import com.aimarketplace.shared.exception.ApiException;
import com.aimarketplace.shared.security.SecurityUtils;
import com.aimarketplace.shared.security.UserPrincipal;
import com.aimarketplace.user.entity.UserProfile;
import com.aimarketplace.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ChatApplicationService {

    private static final EnumSet<OrderStatus> CHAT_ELIGIBLE = EnumSet.of(
            OrderStatus.PAID_ESCROW,
            OrderStatus.IN_PROGRESS,
            OrderStatus.DELIVERED,
            OrderStatus.COMPLETED,
            OrderStatus.DISPUTED
    );

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final MessageReadRepository messageReadRepository;
    private final OrderRepository orderRepository;
    private final UserProfileRepository userProfileRepository;
    private final ChatMessageMapper messageMapper;
    private final PresenceService presenceService;
    private final ChatMessageFanout chatMessageFanout;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void onOrderPaidEscrow(OrderPaidEscrowEvent event) {
        ensureConversation(event.orderId(), event.clientId(), event.freelancerId());
    }

    @Transactional
    public Conversation ensureConversation(Long orderId, Long clientId, Long freelancerId) {
        Optional<Conversation> existing = conversationRepository.findByOrderId(orderId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Conversation conversation = new Conversation();
        conversation.setOrderId(orderId);
        try {
            conversation = conversationRepository.saveAndFlush(conversation);
        } catch (DataIntegrityViolationException ex) {
            return conversationRepository.findByOrderId(orderId)
                    .orElseThrow(() -> ex);
        }

        addParticipant(conversation.getId(), clientId);
        addParticipant(conversation.getId(), freelancerId);

        ChatMessage system = new ChatMessage();
        system.setConversationId(conversation.getId());
        system.setSenderId(null);
        system.setMessageType(MessageType.SYSTEM);
        system.setBody("Payment held in escrow. You can now chat about this order.");
        messageRepository.save(system);
        return conversation;
    }

    @Transactional
    public ConversationResponse getConversationForOrder(Long orderId) {
        UserPrincipal user = SecurityUtils.currentUser();
        Conversation conversation = requireConversationForParticipant(orderId, user.id());
        return toConversationResponse(conversation, user.id());
    }

    @Transactional
    public PageResponse<MessageResponse> listMessages(Long orderId, int page, int size) {
        UserPrincipal user = SecurityUtils.currentUser();
        Conversation conversation = requireConversationForParticipant(orderId, user.id());
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        Page<ChatMessage> result = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversation.getId(), pageable);

        List<ChatMessage> chronological = new ArrayList<>(result.getContent());
        Collections.reverse(chronological);

        Map<Long, String> names = displayNames(chronological.stream()
                .map(ChatMessage::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<MessageResponse> content = chronological.stream()
                .map(m -> toMessageResponse(m, conversation.getOrderId(), user.id(), names))
                .toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public MessageResponse sendMessage(Long orderId, Long senderId, SendMessageRequest request) {
        Conversation conversation = requireConversationForParticipant(orderId, senderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        assertChatEligible(order);

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setMessageType(MessageType.TEXT);
        message.setBody(request.body().trim());
        ChatMessage saved = messageRepository.save(message);

        MessageRead selfRead = new MessageRead();
        selfRead.setMessageId(saved.getId());
        selfRead.setUserId(senderId);
        messageReadRepository.save(selfRead);

        Map<Long, String> names = displayNames(Set.of(senderId));
        MessageResponse response = toMessageResponse(saved, orderId, senderId, names);
        chatMessageFanout.publish(response);
        presenceService.heartbeat(senderId);
        return response;
    }

    @Transactional
    public MessageResponse sendMessageForCurrentUser(Long orderId, SendMessageRequest request) {
        UserPrincipal user = SecurityUtils.currentUser();
        return sendMessage(orderId, user.id(), request);
    }

    @Transactional
    public void markRead(Long messageId) {
        UserPrincipal user = SecurityUtils.currentUser();
        ChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Message not found"));
        if (!participantRepository.existsByConversationIdAndUserId(message.getConversationId(), user.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not a participant in this conversation");
        }
        if (messageReadRepository.existsByMessageIdAndUserId(messageId, user.id())) {
            return;
        }
        MessageRead read = new MessageRead();
        read.setMessageId(messageId);
        read.setUserId(user.id());
        messageReadRepository.save(read);
    }

    @Transactional
    public void markConversationRead(Long orderId) {
        UserPrincipal user = SecurityUtils.currentUser();
        Conversation conversation = requireConversationForParticipant(orderId, user.id());
        Page<ChatMessage> latest = messageRepository.findByConversationIdOrderByCreatedAtDesc(
                conversation.getId(), PageRequest.of(0, 50));
        for (ChatMessage message : latest) {
            if (message.getSenderId() != null && message.getSenderId().equals(user.id())) {
                continue;
            }
            if (!messageReadRepository.existsByMessageIdAndUserId(message.getId(), user.id())) {
                MessageRead read = new MessageRead();
                read.setMessageId(message.getId());
                read.setUserId(user.id());
                messageReadRepository.save(read);
            }
        }
    }

    @Transactional
    public PresenceResponse presence(Long orderId) {
        UserPrincipal user = SecurityUtils.currentUser();
        Conversation conversation = requireConversationForParticipant(orderId, user.id());
        presenceService.heartbeat(user.id());
        return new PresenceResponse(orderId, participantsWithPresence(conversation.getId()));
    }

    @Transactional
    public void assertCanSubscribe(Long orderId, Long userId) {
        requireConversationForParticipant(orderId, userId);
    }

    private void addParticipant(Long conversationId, Long userId) {
        if (participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            return;
        }
        ConversationParticipant participant = new ConversationParticipant();
        participant.setConversationId(conversationId);
        participant.setUserId(userId);
        participantRepository.save(participant);
    }

    private Conversation requireConversationForParticipant(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        if (!order.getClientId().equals(userId) && !order.getFreelancerId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only order participants can access chat");
        }
        assertChatEligible(order);

        Conversation conversation = conversationRepository.findByOrderId(orderId)
                .orElseGet(() -> ensureConversation(orderId, order.getClientId(), order.getFreelancerId()));

        if (!participantRepository.existsByConversationIdAndUserId(conversation.getId(), userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only order participants can access chat");
        }
        return conversation;
    }

    private void assertChatEligible(Order order) {
        if (!CHAT_ELIGIBLE.contains(order.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Chat is available after payment is held in escrow");
        }
    }

    private ConversationResponse toConversationResponse(Conversation conversation, Long currentUserId) {
        long unread = messageReadRepository.countUnread(conversation.getId(), currentUserId);
        return new ConversationResponse(
                conversation.getId(),
                conversation.getOrderId(),
                conversation.getCreatedAt(),
                unread,
                participantsWithPresence(conversation.getId())
        );
    }

    private List<ConversationResponse.ParticipantPresence> participantsWithPresence(Long conversationId) {
        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversationId);
        Set<Long> ids = participants.stream().map(ConversationParticipant::getUserId).collect(Collectors.toSet());
        Map<Long, String> names = displayNames(ids);
        Map<Long, Boolean> online = presenceService.onlineMap(ids);
        return participants.stream()
                .map(p -> new ConversationResponse.ParticipantPresence(
                        p.getUserId(),
                        names.getOrDefault(p.getUserId(), "User #" + p.getUserId()),
                        online.getOrDefault(p.getUserId(), false)
                ))
                .toList();
    }

    private MessageResponse toMessageResponse(
            ChatMessage message,
            Long orderId,
            Long currentUserId,
            Map<Long, String> names
    ) {
        boolean mine = message.getSenderId() != null && message.getSenderId().equals(currentUserId);
        boolean readByMe = mine || messageReadRepository.existsByMessageIdAndUserId(message.getId(), currentUserId);
        String display = message.getSenderId() == null
                ? "System"
                : names.getOrDefault(message.getSenderId(), "User #" + message.getSenderId());
        return messageMapper.toResponse(message, orderId, display, mine, readByMe);
    }

    private Map<Long, String> displayNames(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getDisplayName, (a, b) -> a));
    }
}
