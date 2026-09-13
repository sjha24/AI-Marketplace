package com.aimarketplace.order.mapper;

import com.aimarketplace.order.dto.OrderResponse;
import com.aimarketplace.order.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "jobTitle", source = "jobTitle")
    @Mapping(target = "clientDisplayName", source = "clientDisplayName")
    @Mapping(target = "freelancerDisplayName", source = "freelancerDisplayName")
    OrderResponse toResponse(
            Order order,
            String jobTitle,
            String clientDisplayName,
            String freelancerDisplayName
    );
}
