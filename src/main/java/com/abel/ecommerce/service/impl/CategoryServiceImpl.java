package com.abel.ecommerce.service.impl;

import com.abel.ecommerce.dto.request.CategoryRequest;
import com.abel.ecommerce.entity.Category;
import com.abel.ecommerce.exception.CategoryAlreadyExistsException;
import com.abel.ecommerce.exception.CategoryNotFoundException;
import com.abel.ecommerce.repository.CategoryRepository;
import com.abel.ecommerce.service.CategoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public Category createCategory(CategoryRequest request) {
        // Check if category name already exists
        if (categoryRepository.existsByName(request.getName())) {
            throw CategoryAlreadyExistsException.name(request.getName());
        }

        // Check if parent category exists (if parentId is not 0)
        if (request.getParentId() != null && request.getParentId() > 0) {
            if (!categoryRepository.existsByIdAndStatus(request.getParentId(), 1)) {
                throw new CategoryNotFoundException(request.getParentId(), "parent ID");
            }
        }

        // Create category
        Category category = new Category();
        BeanUtils.copyProperties(request, category);

        return categoryRepository.save(category);
    }

    @Override
    @Transactional
    public Category updateCategory(Long id, CategoryRequest request) {
        // Find existing category
        Category existingCategory = findCategoryById(id);

        // Check if new name conflicts with other categories
        if (!existingCategory.getName().equals(request.getName()) &&
                categoryRepository.existsByName(request.getName())) {
            throw CategoryAlreadyExistsException.name(request.getName());
        }

        // Check if parent category exists (if parentId is not 0)
        if (request.getParentId() != null && request.getParentId() > 0) {
            if (!categoryRepository.existsByIdAndStatus(request.getParentId(), 1)) {
                throw new CategoryNotFoundException(request.getParentId(), "parent ID");
            }
        }

        // Update category
        BeanUtils.copyProperties(request, existingCategory);

        return categoryRepository.save(existingCategory);
    }

    @Override
    public void deleteCategory(Long id) {
        // Check if category exists
        findCategoryById(id);

        // Check if category has subcategories
        long subcategoryCount = categoryRepository.countByParentId(id);
        if (subcategoryCount > 0) {
            throw new RuntimeException("Cannot delete category with subcategories");
        }

        categoryRepository.deleteById(id);
    }

    @Override
    public Category findCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id, "ID"));
    }

    @Override
    public List<Category> findAllCategories() {
        return categoryRepository.findByStatus(1);
    }

    @Override
    public List<Category> findTopLevelCategories() {
        return categoryRepository.findByParentIdAndStatusOrderBySortOrder(0L, 1);
    }

    @Override
    public List<Category> findSubcategories(Long parentId) {
        return categoryRepository.findByParentIdAndStatusOrderBySortOrder(parentId, 1);
    }

    @Override
    public boolean categoryExists(Long categoryId) {
        return categoryRepository.existsByIdAndStatus(categoryId, 1);
    }

    @Override
    public long getSubcategoryCount(Long categoryId) {
        return categoryRepository.countByParentId(categoryId);
    }
}
