package com.aimarketplace.chat.repository;

import com.aimarketplace.chat.entity.MessageRead;
import com.aimarketplace.chat.entity.MessageReadId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageReadRepository extends JpaRepository<MessageRead, MessageReadId> {
    boolean existsByMessageIdAndUserId(Long messageId, Long userId);

    @Query("""
            select count(m.id) from ChatMessage m
            where m.conversationId = :conversationId
              and (m.senderId is null or m.senderId <> :userId)
              and not exists (
                select 1 from MessageRead r
                where r.messageId = m.id and r.userId = :userId
              )
            """)
    long countUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
