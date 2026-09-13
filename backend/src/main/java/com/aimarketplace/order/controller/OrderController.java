package com.aimarketplace.order.controller;

import com.aimarketplace.job.dto.PageResponse;
import com.aimarketplace.order.dto.OrderResponse;
import com.aimarketplace.order.usecase.GetMyOrders;
import com.aimarketplace.order.usecase.GetOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {
    private final GetMyOrders getMyOrders;
    private final GetOrder getOrder;

    @GetMapping("/api/orders/mine")
    public PageResponse<OrderResponse> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return getMyOrders.execute(page, size);
    }

    @GetMapping("/api/orders/{id}")
    public OrderResponse get(@PathVariable Long id) {
        return getOrder.execute(id);
    }
}
