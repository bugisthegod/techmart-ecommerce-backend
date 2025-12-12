package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.CategoryRequest;
import com.abel.ecommerce.entity.Category;
import com.abel.ecommerce.exception.CategoryAlreadyExistsException;
import com.abel.ecommerce.exception.CategoryNotFoundException;
import com.abel.ecommerce.repository.CategoryRepository;
import com.abel.ecommerce.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService Unit Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;
    private CategoryRequest testRequest;

    @BeforeEach
    void setUp() {
        testCategory = buildTestCategory();
        testRequest = buildTestCategoryRequest();
    }

    // ========== CREATE CATEGORY TESTS ==========

    @Test
    @DisplayName("should_CreateCategory_When_ValidRequestWithNoParent")
    void should_CreateCategory_When_ValidRequestWithNoParent() {
        // Arrange
        testRequest.setParentId(0L);
        when(categoryRepository.existsByName(testRequest.getName())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        Category result = categoryService.createCategory(testRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(testRequest.getName());
        assertThat(result.getParentId()).isEqualTo(0L);
        verify(categoryRepository).existsByName(testRequest.getName());
        verify(categoryRepository).save(any(Category.class));
        verify(categoryRepository, never()).existsByIdAndStatus(anyLong(), anyInt());
    }

    @Test
    @DisplayName("should_CreateCategory_When_ValidRequestWithValidParent")
    void should_CreateCategory_When_ValidRequestWithValidParent() {
        // Arrange
        testRequest.setParentId(1L);
        when(categoryRepository.existsByName(testRequest.getName())).thenReturn(false);
        when(categoryRepository.existsByIdAndStatus(1L, 1)).thenReturn(true);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        Category result = categoryService.createCategory(testRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(testRequest.getName());
        verify(categoryRepository).existsByName(testRequest.getName());
        verify(categoryRepository).existsByIdAndStatus(1L, 1);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("should_ThrowCategoryAlreadyExistsException_When_CategoryNameExists")
    void should_ThrowCategoryAlreadyExistsException_When_CategoryNameExists() {
        // Arrange
        when(categoryRepository.existsByName(testRequest.getName())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.createCategory(testRequest))
            .isInstanceOf(CategoryAlreadyExistsException.class)
            .hasMessageContaining("Category already exists with name");

        verify(categoryRepository).existsByName(testRequest.getName());
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("should_ThrowCategoryNotFoundException_When_ParentCategoryNotFound")
    void should_ThrowCategoryNotFoundException_When_ParentCategoryNotFound() {
        // Arrange
        testRequest.setParentId(999L);
        when(categoryRepository.existsByName(testRequest.getName())).thenReturn(false);
        when(categoryRepository.existsByIdAndStatus(999L, 1)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.createCategory(testRequest))
            .isInstanceOf(CategoryNotFoundException.class)
            .hasMessageContaining("parent ID");

        verify(categoryRepository).existsByName(testRequest.getName());
        verify(categoryRepository).existsByIdAndStatus(999L, 1);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("should_NotValidateParent_When_ParentIdIsNull")
    void should_NotValidateParent_When_ParentIdIsNull() {
        // Arrange
        testRequest.setParentId(null);
        when(categoryRepository.existsByName(testRequest.getName())).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        Category result = categoryService.createCategory(testRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(categoryRepository, never()).existsByIdAndStatus(anyLong(), anyInt());
    }

    // ========== UPDATE CATEGORY TESTS ==========

    @Test
    @DisplayName("should_UpdateCategory_When_ValidRequestWithSameName")
    void should_UpdateCategory_When_ValidRequestWithSameName() {
        // Arrange
        Long categoryId = 1L;
        testRequest.setName("Electronics"); // Same as existing
        testCategory.setName("Electronics");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        Category result = categoryService.updateCategory(categoryId, testRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(testRequest.getName());
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).save(any(Category.class));
        verify(categoryRepository, never()).existsByName(anyString());
    }

    @Test
    @DisplayName("should_UpdateCategory_When_ValidRequestWithNewName")
    void should_UpdateCategory_When_ValidRequestWithNewName() {
        // Arrange
        Long categoryId = 1L;
        testCategory.setName("Electronics");
        testRequest.setName("New Electronics");

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByName("New Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        Category result = categoryService.updateCategory(categoryId, testRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).existsByName("New Electronics");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("should_ThrowCategoryNotFoundException_When_CategoryNotFoundForUpdate")
    void should_ThrowCategoryNotFoundException_When_CategoryNotFoundForUpdate() {
        // Arrange
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, testRequest))
            .isInstanceOf(CategoryNotFoundException.class)
            .hasMessageContaining("Category not found");

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("should_ThrowCategoryAlreadyExistsException_When_NewNameAlreadyExists")
    void should_ThrowCategoryAlreadyExistsException_When_NewNameAlreadyExists() {
        // Arrange
        Long categoryId = 1L;
        testCategory.setName("Electronics");
        testRequest.setName("Books"); // Different name that already exists

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByName("Books")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, testRequest))
            .isInstanceOf(CategoryAlreadyExistsException.class)
            .hasMessageContaining("Category already exists");

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).existsByName("Books");
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("should_UpdateCategory_When_ValidParentIdProvided")
    void should_UpdateCategory_When_ValidParentIdProvided() {
        // Arrange
        Long categoryId = 1L;
        testRequest.setParentId(2L);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByIdAndStatus(2L, 1)).thenReturn(true);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        // Act
        Category result = categoryService.updateCategory(categoryId, testRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(categoryRepository).existsByIdAndStatus(2L, 1);
    }

    @Test
    @DisplayName("should_ThrowCategoryNotFoundException_When_ParentNotFoundDuringUpdate")
    void should_ThrowCategoryNotFoundException_When_ParentNotFoundDuringUpdate() {
        // Arrange
        Long categoryId = 1L;
        testRequest.setParentId(999L);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByIdAndStatus(999L, 1)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.updateCategory(categoryId, testRequest))
            .isInstanceOf(CategoryNotFoundException.class)
            .hasMessageContaining("parent ID");

        verify(categoryRepository).existsByIdAndStatus(999L, 1);
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ========== DELETE CATEGORY TESTS ==========

    @Test
    @DisplayName("should_DeleteCategory_When_NoSubcategoriesExist")
    void should_DeleteCategory_When_NoSubcategoriesExist() {
        // Arrange
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.countByParentId(categoryId)).thenReturn(0L);

        // Act
        categoryService.deleteCategory(categoryId);

        // Assert
        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).countByParentId(categoryId);
        verify(categoryRepository).deleteById(categoryId);
    }

    @Test
    @DisplayName("should_ThrowRuntimeException_When_CategoryHasSubcategories")
    void should_ThrowRuntimeException_When_CategoryHasSubcategories() {
        // Arrange
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.countByParentId(categoryId)).thenReturn(3L);

        // Act & Assert
        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Cannot delete category with subcategories");

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository).countByParentId(categoryId);
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("should_ThrowCategoryNotFoundException_When_CategoryNotFoundForDelete")
    void should_ThrowCategoryNotFoundException_When_CategoryNotFoundForDelete() {
        // Arrange
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.deleteCategory(categoryId))
            .isInstanceOf(CategoryNotFoundException.class);

        verify(categoryRepository).findById(categoryId);
        verify(categoryRepository, never()).deleteById(anyLong());
    }

    // ========== FIND CATEGORY BY ID TESTS ==========

    @Test
    @DisplayName("should_ReturnCategory_When_CategoryExistsById")
    void should_ReturnCategory_When_CategoryExistsById() {
        // Arrange
        Long categoryId = 1L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(testCategory));

        // Act
        Category result = categoryService.findCategoryById(categoryId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(categoryId);
        assertThat(result.getName()).isEqualTo(testCategory.getName());
        verify(categoryRepository).findById(categoryId);
    }

    @Test
    @DisplayName("should_ThrowCategoryNotFoundException_When_CategoryNotFoundById")
    void should_ThrowCategoryNotFoundException_When_CategoryNotFoundById() {
        // Arrange
        Long categoryId = 999L;
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> categoryService.findCategoryById(categoryId))
            .isInstanceOf(CategoryNotFoundException.class)
            .hasMessageContaining("Category not found");

        verify(categoryRepository).findById(categoryId);
    }

    // ========== FIND ALL CATEGORIES TESTS ==========

    @Test
    @DisplayName("should_ReturnAllActiveCategories_When_CategoriesExist")
    void should_ReturnAllActiveCategories_When_CategoriesExist() {
        // Arrange
        List<Category> categories = Arrays.asList(
            buildTestCategory(),
            buildTestCategoryWithId(2L, "Books"),
            buildTestCategoryWithId(3L, "Clothing")
        );
        when(categoryRepository.findByStatus(1)).thenReturn(categories);

        // Act
        List<Category> result = categoryService.findAllCategories();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Category::getName)
            .containsExactly("Electronics", "Books", "Clothing");
        verify(categoryRepository).findByStatus(1);
    }

    @Test
    @DisplayName("should_ReturnEmptyList_When_NoActiveCategories")
    void should_ReturnEmptyList_When_NoActiveCategories() {
        // Arrange
        when(categoryRepository.findByStatus(1)).thenReturn(Arrays.asList());

        // Act
        List<Category> result = categoryService.findAllCategories();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(categoryRepository).findByStatus(1);
    }

    // ========== FIND TOP LEVEL CATEGORIES TESTS ==========

    @Test
    @DisplayName("should_ReturnTopLevelCategories_When_TopLevelCategoriesExist")
    void should_ReturnTopLevelCategories_When_TopLevelCategoriesExist() {
        // Arrange
        List<Category> topCategories = Arrays.asList(
            buildTestCategoryWithId(1L, "Electronics"),
            buildTestCategoryWithId(2L, "Books")
        );
        when(categoryRepository.findByParentIdAndStatusOrderBySortOrder(0L, 1))
            .thenReturn(topCategories);

        // Act
        List<Category> result = categoryService.findTopLevelCategories();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(categoryRepository).findByParentIdAndStatusOrderBySortOrder(0L, 1);
    }

    @Test
    @DisplayName("should_ReturnEmptyList_When_NoTopLevelCategories")
    void should_ReturnEmptyList_When_NoTopLevelCategories() {
        // Arrange
        when(categoryRepository.findByParentIdAndStatusOrderBySortOrder(0L, 1))
            .thenReturn(Arrays.asList());

        // Act
        List<Category> result = categoryService.findTopLevelCategories();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(categoryRepository).findByParentIdAndStatusOrderBySortOrder(0L, 1);
    }

    // ========== FIND SUBCATEGORIES TESTS ==========

    @Test
    @DisplayName("should_ReturnSubcategories_When_SubcategoriesExist")
    void should_ReturnSubcategories_When_SubcategoriesExist() {
        // Arrange
        Long parentId = 1L;
        List<Category> subcategories = Arrays.asList(
            buildTestCategoryWithId(10L, "Laptops"),
            buildTestCategoryWithId(11L, "Phones")
        );
        when(categoryRepository.findByParentIdAndStatusOrderBySortOrder(parentId, 1))
            .thenReturn(subcategories);

        // Act
        List<Category> result = categoryService.findSubcategories(parentId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Category::getName)
            .containsExactly("Laptops", "Phones");
        verify(categoryRepository).findByParentIdAndStatusOrderBySortOrder(parentId, 1);
    }

    @Test
    @DisplayName("should_ReturnEmptyList_When_NoSubcategories")
    void should_ReturnEmptyList_When_NoSubcategories() {
        // Arrange
        Long parentId = 1L;
        when(categoryRepository.findByParentIdAndStatusOrderBySortOrder(parentId, 1))
            .thenReturn(Arrays.asList());

        // Act
        List<Category> result = categoryService.findSubcategories(parentId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(categoryRepository).findByParentIdAndStatusOrderBySortOrder(parentId, 1);
    }

    // ========== CATEGORY EXISTS TESTS ==========

    @Test
    @DisplayName("should_ReturnTrue_When_CategoryExistsAndActive")
    void should_ReturnTrue_When_CategoryExistsAndActive() {
        // Arrange
        Long categoryId = 1L;
        when(categoryRepository.existsByIdAndStatus(categoryId, 1)).thenReturn(true);

        // Act
        boolean result = categoryService.categoryExists(categoryId);

        // Assert
        assertThat(result).isTrue();
        verify(categoryRepository).existsByIdAndStatus(categoryId, 1);
    }

    @Test
    @DisplayName("should_ReturnFalse_When_CategoryDoesNotExist")
    void should_ReturnFalse_When_CategoryDoesNotExist() {
        // Arrange
        Long categoryId = 999L;
        when(categoryRepository.existsByIdAndStatus(categoryId, 1)).thenReturn(false);

        // Act
        boolean result = categoryService.categoryExists(categoryId);

        // Assert
        assertThat(result).isFalse();
        verify(categoryRepository).existsByIdAndStatus(categoryId, 1);
    }

    // ========== GET SUBCATEGORY COUNT TESTS ==========

    @Test
    @DisplayName("should_ReturnSubcategoryCount_When_SubcategoriesExist")
    void should_ReturnSubcategoryCount_When_SubcategoriesExist() {
        // Arrange
        Long categoryId = 1L;
        when(categoryRepository.countByParentId(categoryId)).thenReturn(5L);

        // Act
        long result = categoryService.getSubcategoryCount(categoryId);

        // Assert
        assertThat(result).isEqualTo(5L);
        verify(categoryRepository).countByParentId(categoryId);
    }

    @Test
    @DisplayName("should_ReturnZero_When_NoSubcategories")
    void should_ReturnZero_When_NoSubcategories() {
        // Arrange
        Long categoryId = 1L;
        when(categoryRepository.countByParentId(categoryId)).thenReturn(0L);

        // Act
        long result = categoryService.getSubcategoryCount(categoryId);

        // Assert
        assertThat(result).isZero();
        verify(categoryRepository).countByParentId(categoryId);
    }

    // ========== HELPER METHODS ==========

    private Category buildTestCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Electronics");
        category.setParentId(0L);
        category.setIcon("electronics-icon.png");
        category.setSortOrder(1);
        category.setStatus(Category.ACTIVE_CATEGORY);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }

    private Category buildTestCategoryWithId(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setParentId(0L);
        category.setIcon("icon.png");
        category.setSortOrder(1);
        category.setStatus(Category.ACTIVE_CATEGORY);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        return category;
    }

    private CategoryRequest buildTestCategoryRequest() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Electronics");
        request.setParentId(0L);
        request.setIcon("electronics-icon.png");
        request.setSortOrder(1);
        request.setStatus(1);
        return request;
    }
}
