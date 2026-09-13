package com.aimarketplace.payment.repository;

import com.aimarketplace.payment.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    boolean existsByOrderIdAndEntryType(Long orderId, com.aimarketplace.payment.domain.model.LedgerEntryType entryType);
}
