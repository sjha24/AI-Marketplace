package com.aimarketplace.chat.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
public class MessageReadId implements Serializable {
    private Long messageId;
    private Long userId;
}
