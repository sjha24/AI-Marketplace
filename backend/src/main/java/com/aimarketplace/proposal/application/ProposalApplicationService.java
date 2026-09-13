package com.aimarketplace.proposal.application;

import com.aimarketplace.job.domain.model.JobStatus;
import com.aimarketplace.job.dto.PageResponse;
import com.aimarketplace.job.entity.Job;
import com.aimarketplace.job.repository.JobRepository;
import com.aimarketplace.order.application.OrderApplicationService;
import com.aimarketplace.order.dto.OrderResponse;
import com.aimarketplace.order.entity.Order;
import com.aimarketplace.proposal.domain.model.ProposalStatus;
import com.aimarketplace.proposal.dto.ProposalResponse;
import com.aimarketplace.proposal.dto.SubmitProposalRequest;
import com.aimarketplace.proposal.entity.Proposal;
import com.aimarketplace.proposal.event.ProposalAcceptedEvent;
import com.aimarketplace.proposal.mapper.ProposalMapper;
import com.aimarketplace.proposal.repository.ProposalRepository;
import com.aimarketplace.shared.exception.ApiException;
import com.aimarketplace.shared.security.SecurityUtils;
import com.aimarketplace.shared.security.UserPrincipal;
import com.aimarketplace.user.entity.UserProfile;
import com.aimarketplace.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class ProposalApplicationService {

    private static final List<ProposalStatus> OPEN_PROPOSAL_STATUSES =
            List.of(ProposalStatus.SUBMITTED, ProposalStatus.SHORTLISTED);

    private final ProposalRepository proposalRepository;
    private final JobRepository jobRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProposalMapper proposalMapper;
    private final OrderApplicationService orderApplicationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ProposalResponse submit(Long jobId, SubmitProposalRequest request) {
        UserPrincipal user = SecurityUtils.currentUser();
        if (!"FREELANCER".equals(user.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only freelancers can submit proposals");
        }

        Job job = fetchJob(jobId);
        if (job.getStatus() != JobStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "This job is not open for proposals");
        }
        if (Objects.equals(job.getClientId(), user.id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You cannot propose on your own job");
        }
        if (proposalRepository.existsByJobIdAndFreelancerId(jobId, user.id())) {
            throw new ApiException(HttpStatus.CONFLICT, "You already submitted a proposal for this job");
        }

        Proposal proposal = proposalMapper.toEntity(request);
        proposal.setJobId(jobId);
        proposal.setFreelancerId(user.id());
        proposal.setAiGenerated(false);

        try {
            Proposal saved = proposalRepository.saveAndFlush(proposal);
            return toResponse(saved, job);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "You already submitted a proposal for this job");
        }
    }

    @Transactional(readOnly = true)
    public List<ProposalResponse> listForJob(Long jobId) {
        Job job = fetchJob(jobId);
        assertJobOwner(job);
        return proposalRepository.findByJobIdOrderByCreatedAtDesc(jobId).stream()
                .map(proposal -> toResponse(proposal, job))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProposalResponse> listMine(int page, int size) {
        long freelancerId = SecurityUtils.currentUser().id();
        Pageable pageable = PageRequest.of(page, size);
        Page<Proposal> result = proposalRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancerId, pageable);
        return toPage(result);
    }

    @Transactional
    public OrderResponse accept(Long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Proposal not found"));
        Job job = fetchJob(proposal.getJobId());
        assertJobOwner(job);

        if (job.getStatus() != JobStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, "Only open jobs can accept a proposal");
        }
        if (!OPEN_PROPOSAL_STATUSES.contains(proposal.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Only submitted proposals can be accepted");
        }

        proposal.setStatus(ProposalStatus.ACCEPTED);
        proposalRepository.save(proposal);

        proposalRepository.rejectOtherProposals(
                job.getId(),
                proposal.getId(),
                ProposalStatus.REJECTED,
                OPEN_PROPOSAL_STATUSES
        );

        job.setStatus(JobStatus.IN_PROGRESS);
        job.setHiredProposalId(proposal.getId());
        jobRepository.save(job);

        Order order = orderApplicationService.createFromAcceptedProposal(
                job.getId(),
                proposal.getId(),
                job.getClientId(),
                proposal.getFreelancerId(),
                proposal.getBidAmount(),
                proposal.getCurrency()
        );

        eventPublisher.publishEvent(new ProposalAcceptedEvent(
                proposal.getId(),
                job.getId(),
                job.getClientId(),
                proposal.getFreelancerId(),
                order.getId()
        ));

        return orderApplicationService.getOrder(order.getId());
    }

    private Job fetchJob(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    private void assertJobOwner(Job job) {
        if (!Objects.equals(job.getClientId(), SecurityUtils.currentUser().id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the job owner can view or accept proposals");
        }
    }

    private ProposalResponse toResponse(Proposal proposal, Job job) {
        Map<Long, String> names = loadDisplayNames(Set.of(proposal.getFreelancerId()));
        return proposalMapper.toResponse(
                proposal,
                job.getTitle(),
                names.getOrDefault(proposal.getFreelancerId(), "User #" + proposal.getFreelancerId()),
                job.getClientId()
        );
    }

    private PageResponse<ProposalResponse> toPage(Page<Proposal> page) {
        List<Proposal> proposals = page.getContent();
        if (proposals.isEmpty()) {
            return new PageResponse<>(List.of(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }

        Set<Long> jobIds = proposals.stream().map(Proposal::getJobId).collect(Collectors.toSet());
        Map<Long, Job> jobs = jobRepository.findAllById(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, j -> j));
        Set<Long> userIds = proposals.stream()
                .flatMap(p -> Stream.of(p.getFreelancerId(), Optional.ofNullable(jobs.get(p.getJobId())).map(Job::getClientId).orElse(null)))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> names = loadDisplayNames(userIds);

        List<ProposalResponse> content = proposals.stream()
                .map(proposal -> {
                    Job job = jobs.get(proposal.getJobId());
                    String title = job == null ? "Job #" + proposal.getJobId() : job.getTitle();
                    Long clientId = job == null ? null : job.getClientId();
                    return proposalMapper.toResponse(
                            proposal,
                            title,
                            names.getOrDefault(proposal.getFreelancerId(), "User #" + proposal.getFreelancerId()),
                            clientId
                    );
                })
                .toList();
        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private Map<Long, String> loadDisplayNames(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getDisplayName, (a, b) -> a));
    }
}
