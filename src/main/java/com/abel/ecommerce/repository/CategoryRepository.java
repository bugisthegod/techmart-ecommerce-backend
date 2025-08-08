package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Find categories by parent ID
    List<Category> findByParentId(Long parentId);

    // Find categories by status
    List<Category> findByStatus(Integer status);

    // Find categories by parent ID and status
    List<Category> findByParentIdAndStatus(Long parentId, Integer status);

    // Find top level categories (parentId = 0)
    List<Category> findByParentIdAndStatusOrderBySortOrder(Long parentId, Integer status);

    // Check if category exists by ID and status
    boolean existsByIdAndStatus(Long id, Integer status);

    // Find category by name
    Optional<Category> findByName(String name);

    // Check if category name exists
    boolean existsByName(String name);

    // Count subcategories
    long countByParentId(Long parentId);
}
