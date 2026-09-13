package com.aimarketplace.user.repository;

import com.aimarketplace.user.entity.UserSkill;
import com.aimarketplace.user.entity.UserSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserSkillRepository extends JpaRepository<UserSkill, UserSkillId> {
    List<UserSkill> findByIdUserId(Long userId);

    @Modifying
    @Query("delete from UserSkill us where us.id.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
