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
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Category Management", description = "Category creation, update, deletion and query operations")
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Create new category", description = "Create a new product category")
    @PostMapping
    public ResponseEntity<ResponseResult<CategoryResponse>> createCategory(
            @Parameter(description = "Category creation data") 
            @Valid @RequestBody CategoryRequest request) {
        try {
            Category category = categoryService.createCategory(request);
            CategoryResponse response = convertToResponse(category);
            return ResponseEntity.ok(ResponseResult.ok(response));
        } catch (CategoryAlreadyExistsException e) {
            return ResponseEntity.badRequest().body(
                ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage()));
        } catch (CategoryNotFoundException e) {
            return ResponseEntity.badRequest().body(
                ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
    }

    @Operation(summary = "Update category", description = "Update an existing category")
    @PutMapping("/{id}")
    public ResponseEntity<ResponseResult<CategoryResponse>> updateCategory(
            @Parameter(description = "Category ID") @PathVariable Long id,
            @Parameter(description = "Category update data") 
            @Valid @RequestBody CategoryRequest request) {
        try {
            Category category = categoryService.updateCategory(id, request);
            CategoryResponse response = convertToResponse(category);
            return ResponseEntity.ok(ResponseResult.ok(response));
        } catch (CategoryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (CategoryAlreadyExistsException e) {
            return ResponseEntity.badRequest().body(
                ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
    }

    @Operation(summary = "Delete category", description = "Delete a category by ID")
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseResult<String>> deleteCategory(
            @Parameter(description = "Category ID") @PathVariable Long id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(ResponseResult.ok("Category deleted successfully"));
        } catch (CategoryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
    }

    @Operation(summary = "Get category by ID", description = "Retrieve category details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResponseResult<CategoryResponse>> getCategoryById(
            @Parameter(description = "Category ID") @PathVariable Long id) {
        try {
            Category category = categoryService.findCategoryById(id);
            CategoryResponse response = convertToResponse(category);
            return ResponseEntity.ok(ResponseResult.ok(response));
        } catch (CategoryNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
    }

    @Operation(summary = "Get all categories", description = "Retrieve all active categories")
    @GetMapping
    public ResponseEntity<ResponseResult<List<CategoryResponse>>> getAllCategories() {
        try {
            List<Category> categories = categoryService.findAllCategories();
            List<CategoryResponse> responses = categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            return ResponseEntity.ok(ResponseResult.ok(responses));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
    }

    @Operation(summary = "Get top level categories", description = "Retrieve top level categories (parentId = 0)")
    @GetMapping("/top-level")
    public ResponseEntity<ResponseResult<List<CategoryResponse>>> getTopLevelCategories() {
        try {
            List<Category> categories = categoryService.findTopLevelCategories();
            List<CategoryResponse> responses = categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            return ResponseEntity.ok(ResponseResult.ok(responses));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
    }

    @Operation(summary = "Get subcategories", description = "Retrieve subcategories by parent category ID")
    @GetMapping("/{parentId}/subcategories")
    public ResponseEntity<ResponseResult<List<CategoryResponse>>> getSubcategories(
            @Parameter(description = "Parent category ID") @PathVariable Long parentId) {
        try {
            List<Category> categories = categoryService.findSubcategories(parentId);
            List<CategoryResponse> responses = categories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
            return ResponseEntity.ok(ResponseResult.ok(responses));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                ResponseResult.error(ResultCode.COMMON_FAIL));
        }
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
