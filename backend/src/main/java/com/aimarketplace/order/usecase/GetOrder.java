package com.aimarketplace.order.usecase;

import com.aimarketplace.order.application.OrderApplicationService;
import com.aimarketplace.order.dto.OrderResponse;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetOrder {
    private final OrderApplicationService service;

    @Logging
    public OrderResponse execute(Long id) {
        ValidationUtil.create()
                .require(id != null && id > 0, "id", "Order id is required")
                .validate();
        return service.getOrder(id);
    }
}
