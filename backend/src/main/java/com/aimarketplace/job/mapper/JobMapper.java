package com.aimarketplace.job.mapper;

import com.aimarketplace.job.dto.CreateJobRequest;
import com.aimarketplace.job.dto.JobResponse;
import com.aimarketplace.job.dto.JobSummaryResponse;
import com.aimarketplace.job.dto.UpdateJobRequest;
import com.aimarketplace.job.entity.Job;
import com.aimarketplace.job.entity.JobSkill;
import com.aimarketplace.job.entity.JobSkillId;
import com.aimarketplace.user.entity.Skill;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "hiredProposalId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "jobSkills", ignore = true)
    @Mapping(target = "title", source = "title", qualifiedByName = "trim")
    @Mapping(target = "description", source = "description", qualifiedByName = "trim")
    @Mapping(target = "currency", source = "currency", qualifiedByName = "normalizeCurrency")
    Job toEntity(CreateJobRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clientId", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "hiredProposalId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "jobSkills", ignore = true)
    @Mapping(target = "title", source = "title", qualifiedByName = "trim")
    @Mapping(target = "description", source = "description", qualifiedByName = "trim")
    @Mapping(target = "currency", source = "currency", qualifiedByName = "normalizeCurrency")
    void updateEntity(UpdateJobRequest request, @MappingTarget Job job);

    @Mapping(target = "categoryName", source = "categoryName")
    @Mapping(target = "skills", source = "skills")
    JobResponse toResponse(Job job, String categoryName, List<JobResponse.JobSkillResponse> skills);

    @Mapping(target = "skillId", source = "skill.id")
    @Mapping(target = "name", source = "skill.name")
    @Mapping(target = "slug", source = "skill.slug")
    JobResponse.JobSkillResponse toJobSkillResponse(Skill skill);

    @Mapping(target = "categoryName", source = "categoryName")
    @Mapping(target = "skillNames", source = "skillNames")
    JobSummaryResponse toSummary(Job job, String categoryName, List<String> skillNames);

    default JobSkill toJobSkill(Job job, Long skillId) {
        JobSkill jobSkill = new JobSkill();
        jobSkill.setId(new JobSkillId(job.getId(), skillId));
        jobSkill.setJob(job);
        return jobSkill;
    }

    default List<JobResponse.JobSkillResponse> mapSkills(List<JobSkill> jobSkills, Map<Long, Skill> skillsById) {
        return jobSkills.stream()
                .map(jobSkill -> {
                    Skill skill = skillsById.get(jobSkill.getId().getSkillId());
                    return skill == null ? null : toJobSkillResponse(skill);
                })
                .filter(skill -> skill != null)
                .toList();
    }

    default List<String> mapSkillNames(List<JobSkill> jobSkills, Map<Long, Skill> skillsById) {
        return jobSkills.stream()
                .map(jobSkill -> {
                    Skill skill = skillsById.get(jobSkill.getId().getSkillId());
                    return skill == null ? null : skill.getName();
                })
                .filter(name -> name != null)
                .toList();
    }

    @Named("trim")
    default String trim(String value) {
        return value == null ? null : value.trim();
    }

    @Named("normalizeCurrency")
    default String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "INR";
        }
        return currency.trim().toUpperCase();
    }
}
