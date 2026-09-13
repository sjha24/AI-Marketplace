package com.aimarketplace.job.repository;

import com.aimarketplace.job.domain.model.JobStatus;
import com.aimarketplace.job.entity.Job;
import com.aimarketplace.job.entity.JobSkill;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class JobSpecifications {
    private JobSpecifications() {
    }

    public static Specification<Job> withFilters(String q, Long categoryId, Long skillId, JobStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("categoryId"), categoryId));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (skillId != null) {
                Join<Job, JobSkill> skills = root.join("jobSkills", JoinType.INNER);
                predicates.add(cb.equal(skills.get("id").get("skillId"), skillId));
                if (query != null) {
                    query.distinct(true);
                }
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
