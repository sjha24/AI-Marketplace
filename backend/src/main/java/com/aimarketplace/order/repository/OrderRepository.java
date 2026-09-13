package com.aimarketplace.order.repository;

import com.aimarketplace.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    boolean existsByProposalId(Long proposalId);

    boolean existsByOrderNumber(String orderNumber);

    Optional<Order> findByOrderNumber(String orderNumber);

    @Query("""
            select o from Order o
            where o.clientId = :userId or o.freelancerId = :userId
            """)
    Page<Order> findMine(@Param("userId") Long userId, Pageable pageable);
}
