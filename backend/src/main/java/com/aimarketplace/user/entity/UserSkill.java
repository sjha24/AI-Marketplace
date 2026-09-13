package com.aimarketplace.user.entity;

import com.aimarketplace.user.domain.model.SkillLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_skills")
public class UserSkill {
    @EmbeddedId
    private UserSkillId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SkillLevel level = SkillLevel.INTERMEDIATE;
}
