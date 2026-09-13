package com.aimarketplace.chat.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
public class ConversationParticipantId implements Serializable {
    private Long conversationId;
    private Long userId;
}
