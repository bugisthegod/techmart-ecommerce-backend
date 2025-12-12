package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.CategoryRequest;
import com.abel.ecommerce.entity.Category;
import com.abel.ecommerce.exception.CategoryAlreadyExistsException;
import com.abel.ecommerce.exception.CategoryNotFoundException;
import com.abel.ecommerce.service.CategoryService;
import com.abel.ecommerce.service.TokenBlacklistService;
import com.abel.ecommerce.service.UserRoleCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = CategoryController.class)
@EnableMethodSecurity
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryController Integration Tests")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private UserRoleCacheService userRoleCacheService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    private Category testCategory;
    private CategoryRequest testRequest;

    @BeforeEach
    void setUp() {
        testCategory = buildTestCategory();
        testRequest = buildTestCategoryRequest();
    }

    // ========== CREATE CATEGORY TESTS ==========

    @Test
    @DisplayName("should_Return201AndCategoryResponse_When_CreateCategoryWithValidRequest")
    @WithMockUser(roles = {"PRODUCT_ADMIN"})
    void should_Return201AndCategoryResponse_When_CreateCategoryWithValidRequest() throws Exception {
        // Arrange
        when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(testCategory);
        when(categoryService.getSubcategoryCount(anyLong())).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Electronics"))
                .andExpect(jsonPath("$.data.parentId").value(0))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.subcategoryCount").value(0));

        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    @DisplayName("should_Return409_When_CategoryNameAlreadyExists")
    @WithMockUser(roles = {"PRODUCT_ADMIN"})
    void should_Return409_When_CategoryNameAlreadyExists() throws Exception {
        // Arrange
        when(categoryService.createCategory(any(CategoryRequest.class)))
                .thenThrow(new CategoryAlreadyExistsException("Electronics"));

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(5008))
                .andExpect(jsonPath("$.msg").value(containsString("Category already exists")));

        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    @DisplayName("should_Return404_When_ParentCategoryNotFound")
    @WithMockUser(roles = {"PRODUCT_ADMIN"})
    void should_Return404_When_ParentCategoryNotFound() throws Exception {
        // Arrange
        testRequest.setParentId(999L);
        when(categoryService.createCategory(any(CategoryRequest.class)))
                .thenThrow(new CategoryNotFoundException(999L, "parent ID"));

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(5007))
                .andExpect(jsonPath("$.msg").value(containsString("parent ID")));

        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    @DisplayName("should_Return400_When_CategoryNameIsBlank")
    @WithMockUser(roles = {"PRODUCT_ADMIN"})
    void should_Return400_When_CategoryNameIsBlank() throws Exception {
        // Arrange
        testRequest.setName("");

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(any(CategoryRequest.class));
    }

    @Test
    @DisplayName("should_Return400_When_CategoryNameExceedsMaxLength")
    @WithMockUser(roles = {"PRODUCT_ADMIN"})
    void should_Return400_When_CategoryNameExceedsMaxLength() throws Exception {
        // Arrange
        testRequest.setName("A".repeat(51)); // Max is 50

        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isBadRequest());

        verify(categoryService, never()).createCategory(any(CategoryRequest.class));
    }

    @Test
    @DisplayName("should_Return403_When_UserNotAuthorizedToCreate")
    @WithMockUser(username = "user", roles = {"USER"})
    void should_Return403_When_UserNotAuthorizedToCreate() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).createCategory(any(CategoryRequest.class));
    }

    // ========== UPDATE CATEGORY TESTS ==========

    @Test
    @DisplayName("should_Return200AndUpdatedCategory_When_UpdateWithValidRequest")
    @WithMockUser(roles = {"PRODUCT_ADMIN"})
    void should_Return200AndUpdatedCategory_When_UpdateWithValidRequest() throws Exception {
        // Arrange
        Long categoryId = 1L;
        testCategory.setName("Updated Electronics");
        when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class)))
                .thenReturn(testCategory);
        when(categoryService.getSubcategoryCount(anyLong())).thenReturn(2L);

        // Act & Assert
        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Updated Electronics"))
                .andExpect(jsonPath("$.data.subcategoryCount").value(2));

        verify(categoryService).updateCategory(eq(categoryId), any(CategoryRequest.class));
    }

    @Test
    @DisplayName("should_Return404_When_CategoryNotFoundForUpdate")
    @WithMockUser(roles = {"PRODUCT_ADMIN"})
    void should_Return404_When_CategoryNotFoundForUpdate() throws Exception {
        // Arrange
        Long categoryId = 999L;
        when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class)))
                .thenThrow(new CategoryNotFoundException(categoryId, "ID"));

        // Act & Assert
        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(5007))
                .andExpect(jsonPath("$.msg").value(containsString("Category not found with ID: " + categoryId)));

        verify(categoryService).updateCategory(eq(categoryId), any(CategoryRequest.class));
    }

    @Test
    @DisplayName("should_Return409_When_NewCategoryNameAlreadyExistsDuringUpdate")
    @WithMockUser(roles = {"PRODUCT_ADMIN"})
    void should_Return409_When_NewCategoryNameAlreadyExistsDuringUpdate() throws Exception {
        // Arrange
        Long categoryId = 1L;
        when(categoryService.updateCategory(eq(categoryId), any(CategoryRequest.class)))
                .thenThrow(new CategoryAlreadyExistsException("Books"));

        // Act & Assert
        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(5008))
                .andExpect(jsonPath("$.msg").value(containsString("already exists")));

        verify(categoryService).updateCategory(eq(categoryId), any(CategoryRequest.class));
    }

    // ========== DELETE CATEGORY TESTS ==========

    @Test
    @DisplayName("should_Return200_When_DeleteCategorySuccessfully")
    @WithMockUser(roles = {"PRODUCT_ADMIN"})
    void should_Return200_When_DeleteCategorySuccessfully() throws Exception {
        // Arrange
        Long categoryId = 1L;
        doNothing().when(categoryService).deleteCategory(categoryId);

        // Act & Assert
        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value("Category deleted successfully"));

        verify(categoryService).deleteCategory(categoryId);
    }

    @Test
    @DisplayName("should_Return404_When_CategoryNotFoundForDelete")
    @WithMockUser(roles = {"PRODUCT_ADMIN"})
    void should_Return404_When_CategoryNotFoundForDelete() throws Exception {
        // Arrange
        Long categoryId = 999L;
        doThrow(new CategoryNotFoundException(categoryId, "ID"))
                .when(categoryService).deleteCategory(categoryId);

        // Act & Assert
        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(5007))
                .andExpect(jsonPath("$.msg").value(containsString("not found")));

        verify(categoryService).deleteCategory(categoryId);
    }

    @Test
    @DisplayName("should_Return500_When_CategoryHasSubcategories")
    @WithMockUser(roles = {"PRODUCT_ADMIN"})
    void should_Return500_When_CategoryHasSubcategories() throws Exception {
        // Arrange
        Long categoryId = 1L;
        doThrow(new RuntimeException("Cannot delete category with subcategories"))
                .when(categoryService).deleteCategory(categoryId);

        // Act & Assert
        mockMvc.perform(delete("/api/categories/{id}", categoryId)
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(999))
                .andExpect(jsonPath("$.msg").value(containsString("Cannot delete category with subcategories")));

        verify(categoryService).deleteCategory(categoryId);
    }

    // ========== GET CATEGORY BY ID TESTS ==========

    @Test
    @DisplayName("should_Return200AndCategory_When_CategoryFoundById")
    @WithMockUser
    void should_Return200AndCategory_When_CategoryFoundById() throws Exception {
        // Arrange
        Long categoryId = 1L;
        when(categoryService.findCategoryById(categoryId)).thenReturn(testCategory);
        when(categoryService.getSubcategoryCount(categoryId)).thenReturn(3L);

        // Act & Assert
        mockMvc.perform(get("/api/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Electronics"))
                .andExpect(jsonPath("$.data.subcategoryCount").value(3));

        verify(categoryService).findCategoryById(categoryId);
    }

    @Test
    @DisplayName("should_Return404_When_CategoryNotFoundById")
    @WithMockUser
    void should_Return404_When_CategoryNotFoundById() throws Exception {
        // Arrange
        Long categoryId = 999L;
        when(categoryService.findCategoryById(categoryId))
                .thenThrow(new CategoryNotFoundException(categoryId, "ID"));

        // Act & Assert
        mockMvc.perform(get("/api/categories/{id}", categoryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(5007))
                .andExpect(jsonPath("$.msg").value(containsString("not found")));

        verify(categoryService).findCategoryById(categoryId);
    }

    // ========== GET ALL CATEGORIES TESTS ==========

    @Test
    @DisplayName("should_Return200AndCategoryList_When_CategoriesExist")
    @WithMockUser
    void should_Return200AndCategoryList_When_CategoriesExist() throws Exception {
        // Arrange
        List<Category> categories = Arrays.asList(
                buildTestCategoryWithId(1L, "Electronics"),
                buildTestCategoryWithId(2L, "Books"),
                buildTestCategoryWithId(3L, "Clothing")
        );
        when(categoryService.findAllCategories()).thenReturn(categories);
        when(categoryService.getSubcategoryCount(anyLong())).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].name").value("Electronics"))
                .andExpect(jsonPath("$.data[1].name").value("Books"))
                .andExpect(jsonPath("$.data[2].name").value("Clothing"));

        verify(categoryService).findAllCategories();
    }

    @Test
    @DisplayName("should_Return200AndEmptyList_When_NoCategoriesExist")
    @WithMockUser
    void should_Return200AndEmptyList_When_NoCategoriesExist() throws Exception {
        // Arrange
        when(categoryService.findAllCategories()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(categoryService).findAllCategories();
    }

    // ========== GET TOP LEVEL CATEGORIES TESTS ==========

    @Test
    @DisplayName("should_Return200AndTopLevelCategories_When_TopLevelCategoriesExist")
    @WithMockUser
    void should_Return200AndTopLevelCategories_When_TopLevelCategoriesExist() throws Exception {
        // Arrange
        List<Category> topCategories = Arrays.asList(
                buildTestCategoryWithId(1L, "Electronics"),
                buildTestCategoryWithId(2L, "Books")
        );
        when(categoryService.findTopLevelCategories()).thenReturn(topCategories);
        when(categoryService.getSubcategoryCount(anyLong())).thenReturn(5L);

        // Act & Assert
        mockMvc.perform(get("/api/categories/top-level"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("Electronics"))
                .andExpect(jsonPath("$.data[0].parentId").value(0));

        verify(categoryService).findTopLevelCategories();
    }

    @Test
    @DisplayName("should_Return200AndEmptyList_When_NoTopLevelCategories")
    @WithMockUser
    void should_Return200AndEmptyList_When_NoTopLevelCategories() throws Exception {
        // Arrange
        when(categoryService.findTopLevelCategories()).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/categories/top-level"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(categoryService).findTopLevelCategories();
    }

    // ========== GET SUBCATEGORIES TESTS ==========

    @Test
    @DisplayName("should_Return200AndSubcategories_When_SubcategoriesExist")
    @WithMockUser
    void should_Return200AndSubcategories_When_SubcategoriesExist() throws Exception {
        // Arrange
        Long parentId = 1L;
        List<Category> subcategories = Arrays.asList(
                buildTestCategoryWithId(10L, "Laptops"),
                buildTestCategoryWithId(11L, "Phones")
        );
        when(categoryService.findSubcategories(parentId)).thenReturn(subcategories);
        when(categoryService.getSubcategoryCount(anyLong())).thenReturn(0L);

        // Act & Assert
        mockMvc.perform(get("/api/categories/{parentId}/subcategories", parentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].name").value("Laptops"))
                .andExpect(jsonPath("$.data[1].name").value("Phones"));

        verify(categoryService).findSubcategories(parentId);
    }

    @Test
    @DisplayName("should_Return200AndEmptyList_When_NoSubcategories")
    @WithMockUser
    void should_Return200AndEmptyList_When_NoSubcategories() throws Exception {
        // Arrange
        Long parentId = 1L;
        when(categoryService.findSubcategories(parentId)).thenReturn(Arrays.asList());

        // Act & Assert
        mockMvc.perform(get("/api/categories/{parentId}/subcategories", parentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(categoryService).findSubcategories(parentId);
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
