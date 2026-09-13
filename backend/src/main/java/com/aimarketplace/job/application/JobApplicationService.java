package com.aimarketplace.job.application;

import com.aimarketplace.job.dto.*;
import com.aimarketplace.job.entity.Category;
import com.aimarketplace.job.entity.Job;
import com.aimarketplace.job.entity.JobSkill;
import com.aimarketplace.job.mapper.CategoryMapper;
import com.aimarketplace.job.mapper.JobMapper;
import com.aimarketplace.job.repository.CategoryRepository;
import com.aimarketplace.job.repository.JobRepository;
import com.aimarketplace.job.repository.JobSkillRepository;
import com.aimarketplace.job.repository.JobSpecifications;
import com.aimarketplace.shared.exception.ApiException;
import com.aimarketplace.shared.security.SecurityUtils;
import com.aimarketplace.user.entity.Skill;
import com.aimarketplace.user.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class JobApplicationService {

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final CategoryRepository categoryRepository;
    private final SkillRepository skillRepository;
    private final JobMapper jobMapper;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public boolean categoryExists(Long categoryId) {
        return categoryId != null && categoryRepository.existsById(categoryId);
    }

    @Transactional(readOnly = true)
    public boolean allSkillsExist(Collection<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return true;
        }
        List<Long> distinctIds = skillIds.stream().distinct().toList();
        return skillRepository.findAllById(distinctIds).size() == distinctIds.size();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public JobResponse createJob(CreateJobRequest request) {
        long clientId = SecurityUtils.currentUser().id();
        Job job = jobMapper.toEntity(request);
        job.setClientId(clientId);
        Job saved = jobRepository.save(job);
        replaceSkills(saved, request.skillIds());
        return toResponse(fetchJob(saved.getId()));
    }

    @Transactional(readOnly = true)
    public PageResponse<JobSummaryResponse> listJobs(JobListQuery query) {
        Pageable pageable = PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Job> spec = JobSpecifications.withFilters(
                query.q(), query.categoryId(), query.skillId(), query.status());
        Page<Job> page = jobRepository.findAll(spec, pageable);
        return toSummaryPage(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<JobSummaryResponse> listMyJobs(int page, int size) {
        long clientId = SecurityUtils.currentUser().id();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Job> result = jobRepository.findByClientIdOrderByCreatedAtDesc(clientId, pageable);
        return toSummaryPage(result);
    }

    @Transactional(readOnly = true)
    public JobResponse getJob(Long id) {
        return toResponse(fetchJob(id));
    }

    @Transactional
    public JobResponse updateJob(Long id, UpdateJobRequest request) {
        Job job = fetchJob(id);
        assertOwner(job);
        assertOpen(job, "Only open jobs can be updated");
        jobMapper.updateEntity(request, job);
        replaceSkills(job, request.skillIds());
        return toResponse(jobRepository.save(job));
    }

    @Transactional
    public JobResponse cancelJob(Long id) {
        Job job = fetchJob(id);
        assertOwner(job);
        assertOpen(job, "Only open jobs can be cancelled");
        job.setStatus(com.aimarketplace.job.domain.model.JobStatus.CANCELLED);
        return toResponse(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public Job fetchJobEntity(Long id) {
        return fetchJob(id);
    }

    private Job fetchJob(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found"));
    }

    private void assertOwner(Job job) {
        if (!Objects.equals(job.getClientId(), SecurityUtils.currentUser().id())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to modify this job");
        }
    }

    private void assertOpen(Job job, String message) {
        if (job.getStatus() != com.aimarketplace.job.domain.model.JobStatus.OPEN) {
            throw new ApiException(HttpStatus.CONFLICT, message);
        }
    }

    private void replaceSkills(Job job, List<Long> skillIds) {
        jobSkillRepository.deleteByJobId(job.getId());
        jobSkillRepository.flush();
        job.getJobSkills().clear();

        if (skillIds == null || skillIds.isEmpty()) {
            return;
        }

        for (Long skillId : skillIds.stream().distinct().toList()) {
            JobSkill jobSkill = jobMapper.toJobSkill(job, skillId);
            job.getJobSkills().add(jobSkill);
        }
        jobRepository.saveAndFlush(job);
    }

    private JobResponse toResponse(Job job) {
        Map<Long, Category> categories = loadCategories(Set.of(job.getCategoryId()));
        Category category = categories.get(job.getCategoryId());
        String categoryName = category == null ? null : category.getName();

        List<JobSkill> jobSkills = jobSkillRepository.findByIdJobId(job.getId());
        Map<Long, Skill> skillsById = loadSkills(jobSkills);
        List<JobResponse.JobSkillResponse> skills = jobMapper.mapSkills(jobSkills, skillsById);

        return jobMapper.toResponse(job, categoryName, skills);
    }

    private PageResponse<JobSummaryResponse> toSummaryPage(Page<Job> page) {
        List<Job> jobs = page.getContent();
        if (jobs.isEmpty()) {
            return new PageResponse<>(List.of(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }

        Set<Long> jobIds = jobs.stream().map(Job::getId).collect(Collectors.toSet());
        Set<Long> categoryIds = jobs.stream().map(Job::getCategoryId).collect(Collectors.toSet());
        Map<Long, Category> categories = loadCategories(categoryIds);

        List<JobSkill> allJobSkills = jobSkillRepository.findByIdJobIdIn(jobIds);
        Map<Long, List<JobSkill>> skillsByJobId = allJobSkills.stream()
                .collect(Collectors.groupingBy(js -> js.getId().getJobId()));
        Map<Long, Skill> skillsById = loadSkills(allJobSkills);

        List<JobSummaryResponse> content = jobs.stream()
                .map(job -> {
                    Category category = categories.get(job.getCategoryId());
                    String categoryName = category == null ? null : category.getName();
                    List<JobSkill> jobSkills = skillsByJobId.getOrDefault(job.getId(), List.of());
                    List<String> skillNames = jobMapper.mapSkillNames(jobSkills, skillsById);
                    return jobMapper.toSummary(job, categoryName, skillNames);
                })
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private Map<Long, Category> loadCategories(Set<Long> categoryIds) {
        return categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));
    }

    private Map<Long, Skill> loadSkills(List<JobSkill> jobSkills) {
        Set<Long> skillIds = jobSkills.stream()
                .map(js -> js.getId().getSkillId())
                .collect(Collectors.toSet());
        if (skillIds.isEmpty()) {
            return Map.of();
        }
        return skillRepository.findAllById(skillIds).stream()
                .collect(Collectors.toMap(Skill::getId, Function.identity()));
    }

}
