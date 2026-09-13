package com.aimarketplace.job.dto;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        Long parentId
) {
}
