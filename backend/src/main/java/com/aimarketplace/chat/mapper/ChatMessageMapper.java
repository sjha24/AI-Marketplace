package com.aimarketplace.chat.mapper;

import com.aimarketplace.chat.dto.MessageResponse;
import com.aimarketplace.chat.entity.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ChatMessageMapper {

    @Mapping(target = "orderId", source = "orderId")
    @Mapping(target = "senderDisplayName", source = "senderDisplayName")
    @Mapping(target = "mine", source = "mine")
    @Mapping(target = "readByMe", source = "readByMe")
    MessageResponse toResponse(
            ChatMessage message,
            Long orderId,
            String senderDisplayName,
            boolean mine,
            boolean readByMe
    );
}
