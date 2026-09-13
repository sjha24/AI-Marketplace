package com.aimarketplace.order.usecase;

import com.aimarketplace.job.dto.PageResponse;
import com.aimarketplace.order.application.OrderApplicationService;
import com.aimarketplace.order.dto.OrderResponse;
import com.aimarketplace.shared.aop.Logging;
import com.aimarketplace.shared.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetMyOrders {
    private final OrderApplicationService service;

    @Logging
    public PageResponse<OrderResponse> execute(int page, int size) {
        ValidationUtil.create()
                .require(page >= 0, "page", "Page must be 0 or greater")
                .require(size >= 1 && size <= 100, "size", "Size must be between 1 and 100")
                .validate();
        return service.listMine(page, size);
    }
}
