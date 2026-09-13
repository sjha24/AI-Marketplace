package com.aimarketplace.chat.repository;

import com.aimarketplace.chat.entity.ConversationParticipant;
import com.aimarketplace.chat.entity.ConversationParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, ConversationParticipantId> {
    List<ConversationParticipant> findByConversationId(Long conversationId);

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    List<ConversationParticipant> findByUserId(Long userId);
}
