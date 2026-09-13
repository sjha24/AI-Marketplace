package com.aimarketplace.user.mapper;

import com.aimarketplace.user.dto.SkillResponse;
import com.aimarketplace.user.entity.Skill;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SkillMapper {

    SkillResponse toResponse(Skill skill);
}
