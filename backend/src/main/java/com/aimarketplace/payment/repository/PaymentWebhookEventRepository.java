package com.aimarketplace.payment.repository;

import com.aimarketplace.payment.domain.model.PaymentProvider;
import com.aimarketplace.payment.entity.PaymentWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    Optional<PaymentWebhookEvent> findByProviderAndEventId(PaymentProvider provider, String eventId);

    boolean existsByProviderAndEventId(PaymentProvider provider, String eventId);
}
