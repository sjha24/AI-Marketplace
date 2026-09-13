package com.aimarketplace.job.repository;

import com.aimarketplace.job.entity.JobSkill;
import com.aimarketplace.job.entity.JobSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface JobSkillRepository extends JpaRepository<JobSkill, JobSkillId> {
    List<JobSkill> findByIdJobId(Long jobId);

    List<JobSkill> findByIdJobIdIn(Collection<Long> jobIds);

    @Modifying
    @Query("delete from JobSkill js where js.id.jobId = :jobId")
    void deleteByJobId(Long jobId);
}
