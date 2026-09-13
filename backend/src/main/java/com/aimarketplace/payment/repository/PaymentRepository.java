package com.aimarketplace.payment.repository;

import com.aimarketplace.payment.domain.model.PaymentProvider;
import com.aimarketplace.payment.domain.model.PaymentStatus;
import com.aimarketplace.payment.domain.model.PaymentType;
import com.aimarketplace.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByProviderAndProviderOrderId(PaymentProvider provider, String providerOrderId);

    Optional<Payment> findByProviderAndProviderPaymentId(PaymentProvider provider, String providerPaymentId);

    List<Payment> findByOrderIdAndTypeOrderByCreatedAtDesc(Long orderId, PaymentType type);

    boolean existsByOrderIdAndTypeAndStatus(Long orderId, PaymentType type, PaymentStatus status);
}
