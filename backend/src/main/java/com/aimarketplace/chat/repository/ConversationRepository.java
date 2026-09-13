package com.aimarketplace.chat.repository;

import com.aimarketplace.chat.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}
