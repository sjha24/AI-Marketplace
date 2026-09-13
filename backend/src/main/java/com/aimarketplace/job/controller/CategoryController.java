package com.aimarketplace.job.controller;

import com.aimarketplace.job.dto.CategoryResponse;
import com.aimarketplace.job.usecase.ListCategories;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {
    private final ListCategories listCategories;

    @GetMapping("/api/categories")
    public List<CategoryResponse> list() {
        return listCategories.execute();
    }
}
