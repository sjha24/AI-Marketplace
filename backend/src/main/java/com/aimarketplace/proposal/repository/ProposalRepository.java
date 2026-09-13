package com.aimarketplace.proposal.repository;

import com.aimarketplace.proposal.domain.model.ProposalStatus;
import com.aimarketplace.proposal.entity.Proposal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    boolean existsByJobIdAndFreelancerId(Long jobId, Long freelancerId);

    Optional<Proposal> findByJobIdAndFreelancerId(Long jobId, Long freelancerId);

    List<Proposal> findByJobIdOrderByCreatedAtDesc(Long jobId);

    Page<Proposal> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Proposal p
            set p.status = :rejected
            where p.jobId = :jobId
              and p.id <> :acceptedId
              and p.status in :openStatuses
            """)
    int rejectOtherProposals(
            @Param("jobId") Long jobId,
            @Param("acceptedId") Long acceptedId,
            @Param("rejected") ProposalStatus rejected,
            @Param("openStatuses") List<ProposalStatus> openStatuses
    );
}
