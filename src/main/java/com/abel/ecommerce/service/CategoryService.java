package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.CategoryRequest;
import com.abel.ecommerce.entity.Category;

import java.util.List;

public interface CategoryService {

    /**
     * Create a new category
     *
     * @param request Category creation request
     * @return Created category
     */
    Category createCategory(CategoryRequest request);

    /**
     * Update an existing category
     *
     * @param id      Category ID
     * @param request Category update request
     * @return Updated category
     */
    Category updateCategory(Long id, CategoryRequest request);

    /**
     * Delete a category by ID
     *
     * @param id Category ID
     */
    void deleteCategory(Long id);

    /**
     * Find category by ID
     *
     * @param id Category ID
     * @return Category entity
     */
    Category findCategoryById(Long id);

    /**
     * Find all active categories
     *
     * @return List of active categories
     */
    List<Category> findAllCategories();

    /**
     * Find top-level categories (categories without parent)
     *
     * @return List of top-level categories
     */
    List<Category> findTopLevelCategories();

    /**
     * Find subcategories by parent ID
     *
     * @param parentId Parent category ID
     * @return List of subcategories
     */
    List<Category> findSubcategories(Long parentId);

    /**
     * Check if category exists
     *
     * @param categoryId Category ID
     * @return true if category exists and is active
     */
    boolean categoryExists(Long categoryId);

    /**
     * Get count of subcategories
     *
     * @param categoryId Parent category ID
     * @return Number of subcategories
     */
    long getSubcategoryCount(Long categoryId);
}
