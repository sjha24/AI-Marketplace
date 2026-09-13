package com.aimarketplace.shared.event;

import com.aimarketplace.order.event.OrderCreatedEvent;
import com.aimarketplace.payment.event.OrderPaidEscrowEvent;
import com.aimarketplace.payment.event.PaymentCapturedEvent;
import com.aimarketplace.proposal.event.ProposalAcceptedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class DomainEventLogger {
    private static final Logger log = LoggerFactory.getLogger(DomainEventLogger.class);

    @EventListener
    public void onProposalAccepted(ProposalAcceptedEvent event) {
        log.info(
                "DomainEvent ProposalAccepted proposalId={} jobId={} clientId={} freelancerId={} orderId={}",
                event.proposalId(), event.jobId(), event.clientId(), event.freelancerId(), event.orderId()
        );
    }

    @EventListener
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info(
                "DomainEvent OrderCreated orderId={} orderNumber={} jobId={} proposalId={} amount={} {}",
                event.orderId(), event.orderNumber(), event.jobId(), event.proposalId(),
                event.amount(), event.currency()
        );
    }

    @EventListener
    public void onPaymentCaptured(PaymentCapturedEvent event) {
        log.info(
                "DomainEvent PaymentCaptured paymentId={} orderId={} provider={} amount={} {}",
                event.paymentId(), event.orderId(), event.provider(), event.amount(), event.currency()
        );
    }

    @EventListener
    public void onOrderPaidEscrow(OrderPaidEscrowEvent event) {
        log.info(
                "DomainEvent OrderPaidEscrow orderId={} orderNumber={} clientId={} freelancerId={} amount={} {}",
                event.orderId(), event.orderNumber(), event.clientId(), event.freelancerId(),
                event.amount(), event.currency()
        );
    }
}
