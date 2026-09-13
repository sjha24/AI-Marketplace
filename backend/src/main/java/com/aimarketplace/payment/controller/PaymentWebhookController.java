package com.aimarketplace.payment.controller;

import com.aimarketplace.payment.usecase.HandlePaymentWebhook;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class PaymentWebhookController {
    private final HandlePaymentWebhook handlePaymentWebhook;

    @PostMapping("/api/payments/webhook/{provider}")
    public ResponseEntity<Map<String, String>> webhook(
            @PathVariable String provider,
            HttpServletRequest request
    ) throws IOException {
        byte[] bodyBytes = request.getInputStream().readAllBytes();
        String payload = new String(bodyBytes, StandardCharsets.UTF_8);
        Map<String, String> headers = Collections.list(request.getHeaderNames()).stream()
                .collect(Collectors.toMap(
                        name -> name,
                        request::getHeader,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        handlePaymentWebhook.execute(provider, payload, headers);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
