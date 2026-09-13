package com.aimarketplace.job.usecase;

import com.aimarketplace.job.domain.model.ExperienceLevel;
import com.aimarketplace.shared.validation.ValidationUtil;

import java.math.BigDecimal;
import java.util.List;

final class JobRequestValidation {
    private JobRequestValidation() {
    }

    static void validateCommon(
            ValidationUtil validation,
            Long categoryId,
            String title,
            String description,
            BigDecimal budgetMin,
            BigDecimal budgetMax,
            String currency,
            ExperienceLevel experienceLevel,
            List<Long> skillIds
    ) {
        validation.require(categoryId != null, "categoryId", "Category is required");
        validation.require(title != null && !title.isBlank() && title.trim().length() <= 200,
                "title", "Title is required and must not exceed 200 characters");
        validation.require(description != null && !description.isBlank() && description.trim().length() <= 10000,
                "description", "Description is required and must not exceed 10000 characters");
        validation.require(budgetMin != null, "budgetMin", "Minimum budget is required");
        validation.require(budgetMax != null, "budgetMax", "Maximum budget is required");
        if (budgetMin != null) {
            validation.require(budgetMin.compareTo(BigDecimal.ZERO) >= 0,
                    "budgetMin", "Minimum budget cannot be negative");
        }
        if (budgetMax != null) {
            validation.require(budgetMax.compareTo(BigDecimal.ZERO) >= 0,
                    "budgetMax", "Maximum budget cannot be negative");
        }
        if (budgetMin != null && budgetMax != null) {
            validation.require(budgetMin.compareTo(budgetMax) <= 0,
                    "budgetMax", "Maximum budget must be greater than or equal to minimum budget");
        }
        validation.require(currency == null || currency.isBlank() || currency.trim().matches("[A-Za-z]{3}"),
                "currency", "Currency must be a three-letter code");
        validation.require(experienceLevel != null,
                "experienceLevel", "Experience level is required");
        validation.require(skillIds != null && !skillIds.isEmpty(),
                "skillIds", "At least one skill is required");
    }
}
