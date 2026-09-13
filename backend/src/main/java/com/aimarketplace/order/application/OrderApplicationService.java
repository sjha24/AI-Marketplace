package com.aimarketplace.order.application;

import com.aimarketplace.job.dto.PageResponse;
import com.aimarketplace.job.entity.Job;
import com.aimarketplace.job.repository.JobRepository;
import com.aimarketplace.order.dto.OrderResponse;
import com.aimarketplace.order.entity.Order;
import com.aimarketplace.order.event.OrderCreatedEvent;
import com.aimarketplace.order.mapper.OrderMapper;
import com.aimarketplace.order.repository.OrderRepository;
import com.aimarketplace.shared.config.MarketplaceProperties;
import com.aimarketplace.shared.exception.ApiException;
import com.aimarketplace.shared.security.SecurityUtils;
import com.aimarketplace.user.entity.UserProfile;
import com.aimarketplace.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class OrderApplicationService {

    private static final DateTimeFormatter ORDER_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final OrderRepository orderRepository;
    private final JobRepository jobRepository;
    private final UserProfileRepository userProfileRepository;
    private final OrderMapper orderMapper;
    private final MarketplaceProperties marketplaceProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Order createFromAcceptedProposal(
            Long jobId,
            Long proposalId,
            Long clientId,
            Long freelancerId,
            BigDecimal amount,
            String currency
    ) {
        if (orderRepository.existsByProposalId(proposalId)) {
            throw new ApiException(HttpStatus.CONFLICT, "An order already exists for this proposal");
        }

        BigDecimal feePercent = marketplaceProperties.platformFeePercent() == null
                ? BigDecimal.TEN
                : marketplaceProperties.platformFeePercent();
        BigDecimal platformFee = amount
                .multiply(feePercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal payout = amount.subtract(platformFee);

        Order order = new Order();
        order.setOrderNumber(nextOrderNumber());
        order.setJobId(jobId);
        order.setProposalId(proposalId);
        order.setClientId(clientId);
        order.setFreelancerId(freelancerId);
        order.setAmount(amount);
        order.setPlatformFee(platformFee);
        order.setFreelancerPayout(payout);
        order.setCurrency(currency);
        Order saved = orderRepository.save(order);

        eventPublisher.publishEvent(new OrderCreatedEvent(
                saved.getId(),
                saved.getOrderNumber(),
                saved.getJobId(),
                saved.getProposalId(),
                saved.getClientId(),
                saved.getFreelancerId(),
                saved.getAmount(),
                saved.getCurrency()
        ));
        return saved;
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> listMine(int page, int size) {
        long userId = SecurityUtils.currentUser().id();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> result = orderRepository.findMine(userId, pageable);
        return toPage(result);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Order not found"));
        assertParticipant(order);
        return toResponse(order);
    }

    private void assertParticipant(Order order) {
        long userId = SecurityUtils.currentUser().id();
        if (!Objects.equals(order.getClientId(), userId) && !Objects.equals(order.getFreelancerId(), userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have access to this order");
        }
    }

    private String nextOrderNumber() {
        String date = LocalDate.now(ZoneOffset.UTC).format(ORDER_DATE);
        for (int attempt = 0; attempt < 8; attempt++) {
            String candidate = "ORD-" + date + "-" + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        return "ORD-" + date + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PageResponse<OrderResponse> toPage(Page<Order> page) {
        List<Order> orders = page.getContent();
        if (orders.isEmpty()) {
            return new PageResponse<>(List.of(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
        Map<Long, String> jobTitles = loadJobTitles(orders.stream().map(Order::getJobId).collect(Collectors.toSet()));
        Map<Long, String> names = loadDisplayNames(orders.stream()
                .flatMap(o -> Stream.of(o.getClientId(), o.getFreelancerId()))
                .collect(Collectors.toSet()));
        List<OrderResponse> content = orders.stream()
                .map(order -> orderMapper.toResponse(
                        order,
                        jobTitles.getOrDefault(order.getJobId(), "Job #" + order.getJobId()),
                        names.getOrDefault(order.getClientId(), "User #" + order.getClientId()),
                        names.getOrDefault(order.getFreelancerId(), "User #" + order.getFreelancerId())
                ))
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private OrderResponse toResponse(Order order) {
        Map<Long, String> jobTitles = loadJobTitles(Set.of(order.getJobId()));
        Map<Long, String> names = loadDisplayNames(Set.of(order.getClientId(), order.getFreelancerId()));
        return orderMapper.toResponse(
                order,
                jobTitles.getOrDefault(order.getJobId(), "Job #" + order.getJobId()),
                names.getOrDefault(order.getClientId(), "User #" + order.getClientId()),
                names.getOrDefault(order.getFreelancerId(), "User #" + order.getFreelancerId())
        );
    }

    private Map<Long, String> loadJobTitles(Set<Long> jobIds) {
        return jobRepository.findAllById(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, Job::getTitle));
    }

    private Map<Long, String> loadDisplayNames(Set<Long> userIds) {
        return userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getDisplayName, (a, b) -> a));
    }
}
