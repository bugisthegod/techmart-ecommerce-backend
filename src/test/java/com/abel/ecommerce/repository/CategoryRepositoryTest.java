package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for CategoryRepository
 *
 * Tests the hierarchical category structure with parent-child relationships
 */
@DataJpaTest
@DisplayName("CategoryRepository Integration Tests")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private Category rootCategory1;
    private Category rootCategory2;
    private Category subCategory1;
    private Category subCategory2;
    private Category inactiveCategory;

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();

        // Root categories (parentId = 0)
        rootCategory1 = new Category();
        rootCategory1.setName("Electronics");
        rootCategory1.setParentId(0L);
        rootCategory1.setIcon("electronics.png");
        rootCategory1.setSortOrder(1);
        rootCategory1.setStatus(Category.ACTIVE_CATEGORY);

        rootCategory2 = new Category();
        rootCategory2.setName("Clothing");
        rootCategory2.setParentId(0L);
        rootCategory2.setIcon("clothing.png");
        rootCategory2.setSortOrder(2);
        rootCategory2.setStatus(Category.ACTIVE_CATEGORY);

        categoryRepository.save(rootCategory1);
        categoryRepository.save(rootCategory2);

        // Subcategories
        subCategory1 = new Category();
        subCategory1.setName("Laptops");
        subCategory1.setParentId(rootCategory1.getId());
        subCategory1.setIcon("laptop.png");
        subCategory1.setSortOrder(1);
        subCategory1.setStatus(Category.ACTIVE_CATEGORY);

        subCategory2 = new Category();
        subCategory2.setName("Smartphones");
        subCategory2.setParentId(rootCategory1.getId());
        subCategory2.setIcon("phone.png");
        subCategory2.setSortOrder(2);
        subCategory2.setStatus(Category.ACTIVE_CATEGORY);

        inactiveCategory = new Category();
        inactiveCategory.setName("Discontinued");
        inactiveCategory.setParentId(rootCategory1.getId());
        inactiveCategory.setSortOrder(3);
        inactiveCategory.setStatus(Category.NONACTIVE_CATEGORY);

        categoryRepository.save(subCategory1);
        categoryRepository.save(subCategory2);
        categoryRepository.save(inactiveCategory);
    }

    // ========== BASIC CRUD TESTS ==========

    @Test
    @DisplayName("Should save and retrieve category")
    void saveAndFindCategory() {
        Category newCategory = new Category();
        newCategory.setName("Books");
        newCategory.setParentId(0L);
        newCategory.setIcon("books.png");

        Category saved = categoryRepository.save(newCategory);
        Optional<Category> found = categoryRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Books");
        assertThat(found.get().getStatus()).isEqualTo(Category.ACTIVE_CATEGORY);
    }

    @Test
    @DisplayName("Should return empty when category not found")
    void findById_NotFound() {
        Optional<Category> result = categoryRepository.findById(999L);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should delete category")
    void deleteCategory() {
        Long categoryId = subCategory1.getId();
        categoryRepository.deleteById(categoryId);
        Optional<Category> deleted = categoryRepository.findById(categoryId);
        assertThat(deleted).isEmpty();
    }

    // ========== CUSTOM QUERY TESTS ==========

    @Test
    @DisplayName("Should find categories by parent ID")
    void findByParentId() {
        List<Category> rootCategories = categoryRepository.findByParentId(0L);
        List<Category> subCategories = categoryRepository.findByParentId(rootCategory1.getId());

        assertThat(rootCategories).hasSize(2);
        assertThat(rootCategories).extracting(Category::getName)
                .containsExactlyInAnyOrder("Electronics", "Clothing");

        assertThat(subCategories).hasSize(3); // Including inactive
        assertThat(subCategories).extracting(Category::getName)
                .containsExactlyInAnyOrder("Laptops", "Smartphones", "Discontinued");
    }

    @Test
    @DisplayName("Should find categories by status")
    void findByStatus() {
        List<Category> activeCategories = categoryRepository.findByStatus(Category.ACTIVE_CATEGORY);
        List<Category> inactiveCategories = categoryRepository.findByStatus(Category.NONACTIVE_CATEGORY);

        assertThat(activeCategories).hasSize(4); // 2 root + 2 subcategories
        assertThat(inactiveCategories).hasSize(1);
        assertThat(inactiveCategories.get(0).getName()).isEqualTo("Discontinued");
    }

    @Test
    @DisplayName("Should find categories by parent ID and status")
    void findByParentIdAndStatus() {
        List<Category> activeSubCategories = categoryRepository.findByParentIdAndStatus(
                rootCategory1.getId(),
                Category.ACTIVE_CATEGORY
        );

        assertThat(activeSubCategories).hasSize(2);
        assertThat(activeSubCategories).extracting(Category::getName)
                .containsExactlyInAnyOrder("Laptops", "Smartphones");
    }

    @Test
    @DisplayName("Should find categories ordered by sort order")
    void findByParentIdAndStatusOrderBySortOrder() {
        List<Category> sortedCategories = categoryRepository.findByParentIdAndStatusOrderBySortOrder(
                0L,
                Category.ACTIVE_CATEGORY
        );

        assertThat(sortedCategories).hasSize(2);
        assertThat(sortedCategories.get(0).getName()).isEqualTo("Electronics");
        assertThat(sortedCategories.get(1).getName()).isEqualTo("Clothing");
        assertThat(sortedCategories.get(0).getSortOrder())
                .isLessThan(sortedCategories.get(1).getSortOrder());
    }

    @Test
    @DisplayName("Should check if category exists by ID and status")
    void existsByIdAndStatus() {
        boolean activeExists = categoryRepository.existsByIdAndStatus(
                rootCategory1.getId(),
                Category.ACTIVE_CATEGORY
        );
        boolean inactiveExists = categoryRepository.existsByIdAndStatus(
                inactiveCategory.getId(),
                Category.ACTIVE_CATEGORY
        );

        assertThat(activeExists).isTrue();
        assertThat(inactiveExists).isFalse();
    }

    @Test
    @DisplayName("Should find category by name")
    void findByName() {
        Optional<Category> found = categoryRepository.findByName("Electronics");
        Optional<Category> notFound = categoryRepository.findByName("NonExistent");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(rootCategory1.getId());
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("Should check if category name exists")
    void existsByName() {
        boolean exists = categoryRepository.existsByName("Electronics");
        boolean notExists = categoryRepository.existsByName("NonExistent");

        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should count subcategories by parent ID")
    void countByParentId() {
        long rootCount = categoryRepository.countByParentId(0L);
        long electronicsSubCount = categoryRepository.countByParentId(rootCategory1.getId());
        long clothingSubCount = categoryRepository.countByParentId(rootCategory2.getId());

        assertThat(rootCount).isEqualTo(2);
        assertThat(electronicsSubCount).isEqualTo(3); // Including inactive
        assertThat(clothingSubCount).isEqualTo(0);
    }

    // ========== HIERARCHICAL STRUCTURE TESTS ==========

    @Test
    @DisplayName("Should verify hierarchical parent-child relationship")
    void hierarchicalRelationship() {
        // Get root category
        Category root = categoryRepository.findById(rootCategory1.getId()).orElseThrow();

        // Get its children
        List<Category> children = categoryRepository.findByParentId(root.getId());

        assertThat(root.getParentId()).isEqualTo(0L);
        assertThat(children).hasSize(3);
        assertThat(children).allMatch(c -> c.getParentId().equals(root.getId()));
    }

    @Test
    @DisplayName("Should handle multi-level hierarchy")
    void multiLevelHierarchy() {
        // Create third-level category
        Category thirdLevel = new Category();
        thirdLevel.setName("Gaming Laptops");
        thirdLevel.setParentId(subCategory1.getId());
        thirdLevel.setStatus(Category.ACTIVE_CATEGORY);
        categoryRepository.save(thirdLevel);

        // Verify three levels exist
        List<Category> level1 = categoryRepository.findByParentId(0L);
        List<Category> level2 = categoryRepository.findByParentId(rootCategory1.getId());
        List<Category> level3 = categoryRepository.findByParentId(subCategory1.getId());

        assertThat(level1).isNotEmpty();
        assertThat(level2).isNotEmpty();
        assertThat(level3).hasSize(1);
        assertThat(level3.get(0).getName()).isEqualTo("Gaming Laptops");
    }

    // ========== ENTITY LIFECYCLE TESTS ==========

    @Test
    @DisplayName("Should set timestamps and defaults on save")
    void entityLifecycle_Timestamps() {
        Category newCategory = new Category();
        newCategory.setName("Test Category");

        Category saved = categoryRepository.save(newCategory);

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(Category.ACTIVE_CATEGORY);
        assertThat(saved.getSortOrder()).isEqualTo(0);
        assertThat(saved.getParentId()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should update timestamps on entity update")
    void entityLifecycle_UpdateTimestamp() throws InterruptedException {
        Category category = categoryRepository.findById(rootCategory1.getId()).orElseThrow();
        Thread.sleep(100);

        category.setName("Updated Electronics");
        Category updated = categoryRepository.saveAndFlush(category);

        assertThat(updated.getName()).isEqualTo("Updated Electronics");
        assertThat(updated.getCreatedAt()).isEqualTo(category.getCreatedAt());
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("Should handle empty result for non-existent parent")
    void findByParentId_EmptyResult() {
        List<Category> results = categoryRepository.findByParentId(999L);
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle duplicate name check")
    void duplicateName() {
        boolean existsBefore = categoryRepository.existsByName("NewCategory");
        assertThat(existsBefore).isFalse();

        Category category = new Category();
        category.setName("NewCategory");
        categoryRepository.save(category);

        boolean existsAfter = categoryRepository.existsByName("NewCategory");
        assertThat(existsAfter).isTrue();
    }

    @Test
    @DisplayName("Should find all root categories")
    void findAllRootCategories() {
        List<Category> rootCategories = categoryRepository.findByParentIdAndStatus(
                0L,
                Category.ACTIVE_CATEGORY
        );

        assertThat(rootCategories).hasSize(2);
        assertThat(rootCategories).allMatch(c -> c.getParentId().equals(0L));
        assertThat(rootCategories).allMatch(c -> c.getStatus().equals(Category.ACTIVE_CATEGORY));
    }
}