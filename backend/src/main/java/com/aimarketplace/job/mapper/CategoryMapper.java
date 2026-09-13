package com.aimarketplace.job.mapper;

import com.aimarketplace.job.dto.CategoryResponse;
import com.aimarketplace.job.entity.Category;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryResponse toResponse(Category category);
}
