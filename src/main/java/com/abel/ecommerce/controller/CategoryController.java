package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.CategoryRequest;
import com.abel.ecommerce.dto.response.CategoryResponse;
import com.abel.ecommerce.entity.Category;
import com.abel.ecommerce.exception.CategoryAlreadyExistsException;
import com.abel.ecommerce.exception.CategoryNotFoundException;
import com.abel.ecommerce.service.CategoryService;
import com.abel.ecommerce.utils.ResponseResult;
import com.abel.ecommerce.utils.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Category Management", description = "Category creation, update, deletion and query operations")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Create new category", description = "Create a new product category")
    @PreAuthorize("hasRole('PRODUCT_ADMIN') or hasRole('SUPER_ADMIN')")
    @PostMapping
    public ResponseResult<CategoryResponse> createCategory(
            @Parameter(description = "Category creation data")
            @Valid @RequestBody CategoryRequest request) {
        Category category = categoryService.createCategory(request);
        CategoryResponse response = convertToResponse(category);
        return ResponseResult.ok(response);
    }

    @Operation(summary = "Update category", description = "Update an existing category")
    @PutMapping("/{id}")
    public ResponseResult<CategoryResponse> updateCategory(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @Parameter(description = "Category update data")
            @Valid @RequestBody CategoryRequest request) {
        Category category = categoryService.updateCategory(id, request);
        CategoryResponse response = convertToResponse(category);
        return ResponseResult.ok(response);
    }

    @Operation(summary = "Delete category", description = "Delete a category by ID")
    @DeleteMapping("/{id}")
    public ResponseResult<String> deleteCategory(
            @Parameter(description = "Category ID") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseResult.ok("Category deleted successfully");
    }

    @Operation(summary = "Get category by ID", description = "Retrieve category details by ID")
    @GetMapping("/{id}")
    public ResponseResult<CategoryResponse> getCategoryById(
            @Parameter(description = "Category ID") @PathVariable Long id) {
        Category category = categoryService.findCategoryById(id);
        CategoryResponse response = convertToResponse(category);
        return ResponseResult.ok(response);
    }

    @Operation(summary = "Get all categories", description = "Retrieve all active categories")
    @GetMapping
    public ResponseResult<List<CategoryResponse>> getAllCategories() {
        List<Category> categories = categoryService.findAllCategories();
        List<CategoryResponse> responses = categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseResult.ok(responses);
    }

    @Operation(summary = "Get top level categories", description = "Retrieve top level categories (parentId = 0)")
    @GetMapping("/top-level")
    public ResponseResult<List<CategoryResponse>> getTopLevelCategories() {
        List<Category> categories = categoryService.findTopLevelCategories();
        List<CategoryResponse> responses = categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseResult.ok(responses);
    }

    @Operation(summary = "Get subcategories", description = "Retrieve subcategories by parent category ID")
    @GetMapping("/{parentId}/subcategories")
    public ResponseResult<List<CategoryResponse>> getSubcategories(
            @Parameter(description = "Parent category ID") @PathVariable Long parentId) {
        List<Category> categories = categoryService.findSubcategories(parentId);
        List<CategoryResponse> responses = categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseResult.ok(responses);
    }

    private CategoryResponse convertToResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        BeanUtils.copyProperties(category, response);
        
        // Add subcategory count
        long subcategoryCount = categoryService.getSubcategoryCount(category.getId());
        response.setSubcategoryCount(subcategoryCount);
        
        return response;
    }
}
