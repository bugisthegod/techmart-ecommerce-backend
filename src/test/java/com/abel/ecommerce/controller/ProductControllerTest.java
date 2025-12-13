package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.ProductRequest;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.exception.ProductNotFoundException;
import com.abel.ecommerce.filter.RateLimitFilter;
import com.abel.ecommerce.service.ProductService;
import com.abel.ecommerce.service.TokenBlacklistService;
import com.abel.ecommerce.service.UserRoleCacheService;
import com.abel.ecommerce.utils.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for ProductController
 *
 * Key concepts demonstrated:
 * - @WebMvcTest: Sets up Spring MVC test context with only web layer
 * - @MockBean: Creates mock beans in Spring context
 * - MockMvc: Simulates HTTP requests and validates responses
 * - @WithMockUser: Provides mock authentication for secured endpoints
 * - JSON serialization/deserialization
 * - HTTP status code validation
 * - JSON path assertions
 *
 * Note: This only tests the web layer, service layer is mocked
 */
@EnableMethodSecurity
@WebMvcTest(controllers = ProductController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitFilter.class))
@DisplayName("ProductController Web Layer Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private UserRoleCacheService userRoleCacheService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    private Product testProduct;
    private ProductRequest testProductRequest;

    @BeforeEach
    void setUp() {
        // Create test product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setCategoryId(10L);
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setOriginalPrice(new BigDecimal("129.99"));
        testProduct.setDescription("Test Description");
        testProduct.setMainImage("test-image.jpg");
        testProduct.setStock(100);
        testProduct.setSales(10);
        testProduct.setStatus(Product.ACTIVE_PRODUCT);

        // Create test product request
        testProductRequest = new ProductRequest();
        testProductRequest.setName("New Product");
        testProductRequest.setCategoryId(20L);
        testProductRequest.setPrice(new BigDecimal("49.99"));
        testProductRequest.setOriginalPrice(new BigDecimal("69.99"));
        testProductRequest.setMainImage("new-image.jpg");
        testProductRequest.setStock(50);
    }

    // ========== CREATE PRODUCT TESTS ==========

    @Test
    @DisplayName("Should create product successfully with admin role")
    @WithMockUser(username = "admin", roles = {"PRODUCT_ADMIN"})
    void createProduct_Success() throws Exception {
        // Arrange
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(testProduct);

        // Act & Assert
        mockMvc.perform(post("/api/products/createProduct")
                        .with(csrf()) // Include CSRF token for security
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProductRequest)))
                .andDo(print()) // Print request/response for debugging
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data").value(containsString("Product created successfully")))
                .andExpect(jsonPath("$.data").value(containsString("1")));

        verify(productService, times(1)).createProduct(any(ProductRequest.class));
    }

    @Test
    @DisplayName("Should return 400 when product name is blank")
    @WithMockUser(username = "admin", roles = {"PRODUCT_ADMIN"})
    void createProduct_BlankName_BadRequest() throws Exception {
        // Arrange
        testProductRequest.setName(""); // Blank name violates @NotBlank validation

        // Act & Assert
        mockMvc.perform(post("/api/products/createProduct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProductRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ResultCode.PARAM_NOT_VALID.getCode()))
                .andExpect(jsonPath("$.data").doesNotExist());



        verify(productService, never()).createProduct(any(ProductRequest.class));
    }

    @Test
    @DisplayName("Should return 403 when user lacks PRODUCT_ADMIN role")
    @WithMockUser(username = "user", roles = {"USER"})
    void createProduct_Forbidden_WithoutAdminRole() throws Exception {

        // Act & Assert
        mockMvc.perform(post("/api/products/createProduct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProductRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(productService, never()).createProduct(any(ProductRequest.class));
    }

    // ========== GET PRODUCT BY ID TESTS ==========

    @Test
    @DisplayName("Should find product by ID successfully")
    @WithMockUser
    void findProductById_Success() throws Exception {
        // Arrange
        when(productService.findProductById(1L)).thenReturn(testProduct);

        // Act & Assert
        mockMvc.perform(get("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Test Product"))
                .andExpect(jsonPath("$.data.price").value(99.99))
                .andExpect(jsonPath("$.data.categoryId").value(10))
                .andExpect(jsonPath("$.data.stock").value(100));

        verify(productService, times(1)).findProductById(1L);
    }

    @Test
    @DisplayName("Should return error when product not found")
    @WithMockUser
    void findProductById_NotFound() throws Exception {
        // Arrange
        when(productService.findProductById(999L))
                .thenThrow(new ProductNotFoundException(999L, "ID"));

        // Act & Assert
        mockMvc.perform(get("/api/products/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ResultCode.PRODUCT_NOT_EXIST.getCode()))
                .andExpect(jsonPath("$.msg").exists());

        verify(productService, times(1)).findProductById(999L);
    }

    // ========== UPDATE PRODUCT TESTS ==========

    @Test
    @DisplayName("Should update product successfully")
    @WithMockUser
    void updateProduct_Success() throws Exception {
        // Arrange
        Product updatedProduct = new Product();
        updatedProduct.setId(1L);
        updatedProduct.setName("Updated Product");
        updatedProduct.setPrice(new BigDecimal("149.99"));
        updatedProduct.setCategoryId(20L);
        updatedProduct.setOriginalPrice(new BigDecimal("199.99"));
        updatedProduct.setMainImage("updated.jpg");

        when(productService.updateProduct(any(ProductRequest.class), eq(1L)))
                .thenReturn(updatedProduct);

        // Act & Assert
        mockMvc.perform(put("/api/products/{id}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProductRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Updated Product"))
                .andExpect(jsonPath("$.data.price").value(149.99));

        verify(productService, times(1)).updateProduct(any(ProductRequest.class), eq(1L));
    }

    @Test
    @DisplayName("Should return error when updating non-existent product")
    @WithMockUser
    void updateProduct_NotFound() throws Exception {
        // Arrange
        when(productService.updateProduct(any(ProductRequest.class), eq(999L)))
                .thenThrow(new ProductNotFoundException(999L, "ID"));

        // Act & Assert
        mockMvc.perform(put("/api/products/{id}", 999L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProductRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ResultCode.PRODUCT_NOT_EXIST.getCode()));

        verify(productService, times(1)).updateProduct(any(ProductRequest.class), eq(999L));
    }

    // ========== DELETE PRODUCT TESTS ==========

    @Test
    @DisplayName("Should delete product successfully")
    @WithMockUser
    void deleteProduct_Success() throws Exception {
        // Arrange
        doNothing().when(productService).deleteProduct(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/products/{id}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        verify(productService, times(1)).deleteProduct(1L);
    }

    @Test
    @DisplayName("Should return error when deleting non-existent product")
    @WithMockUser
    void deleteProduct_NotFound() throws Exception {
        // Arrange
        doThrow(new ProductNotFoundException(999L, "ID"))
                .when(productService).deleteProduct(999L);

        // Act & Assert
        mockMvc.perform(delete("/api/products/{id}", 999L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ResultCode.PRODUCT_NOT_EXIST.getCode()));

        verify(productService, times(1)).deleteProduct(999L);
    }

    // ========== PAGINATION TESTS ==========

    @Test
    @DisplayName("Should find products with pagination")
    @WithMockUser
    void findProductsWithPagination_Success() throws Exception {
        // Arrange
        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setCategoryId(10L);
        product2.setPrice(new BigDecimal("79.99"));
        product2.setOriginalPrice(new BigDecimal("99.99"));
        product2.setMainImage("product2.jpg");

        List<Product> products = Arrays.asList(testProduct, product2);
        Page<Product> productPage = new PageImpl<>(products, PageRequest.of(0, 10), 2);

        when(productService.findProductsWithPagination(
                eq(10L),
                eq(1),
                any(PageRequest.class)
        )).thenReturn(productPage);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                        .param("categoryId", "10")
                        .param("status", "1")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "createdAt")
                        .param("sortDir", "desc")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].name").value("Test Product"))
                .andExpect(jsonPath("$.data.content[1].name").value("Product 2"));

        verify(productService, times(1)).findProductsWithPagination(
                eq(10L),
                eq(1),
                any(PageRequest.class)
        );
    }

    @Test
    @DisplayName("Should find products with default pagination parameters")
    @WithMockUser
    void findProductsWithPagination_DefaultParameters() throws Exception {
        // Arrange
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productService.findProductsWithPagination(
                eq(null),
                eq(1), // Default status
                any(PageRequest.class)
        )).thenReturn(productPage);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        verify(productService, times(1)).findProductsWithPagination(
                eq(null),
                eq(1),
                any(PageRequest.class)
        );
    }

    @Test
    @DisplayName("Should return empty page when no products found")
    @WithMockUser
    void findProductsWithPagination_EmptyResult() throws Exception {
        // Arrange
        Page<Product> emptyPage = new PageImpl<>(Arrays.asList());
        when(productService.findProductsWithPagination(
                eq(999L),
                eq(1),
                any(PageRequest.class)
        )).thenReturn(emptyPage);

        // Act & Assert
        mockMvc.perform(get("/api/products")
                        .param("categoryId", "999")
                        .param("status", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ========== SEARCH PRODUCTS TESTS ==========

    @Test
    @DisplayName("Should search products by name")
    @WithMockUser
    void searchProducts_Success() throws Exception {
        // Arrange
        List<Product> products = Arrays.asList(testProduct);
        when(productService.searchProducts("Test")).thenReturn(products);

        // Act & Assert
        mockMvc.perform(get("/api/products/search")
                        .param("name", "Test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name").value("Test Product"));

        verify(productService, times(1)).searchProducts("Test");
    }

    // ========== VALIDATION TESTS ==========

    @Test
    @DisplayName("Should validate required fields in request body")
    @WithMockUser(roles = "PRODUCT_ADMIN")
    void createProduct_InvalidRequest_ValidationError() throws Exception {
        // Arrange
        ProductRequest invalidRequest = new ProductRequest();
        // Missing required fields: name and mainImage

        // Act & Assert
        mockMvc.perform(post("/api/products/createProduct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(ProductRequest.class));
    }

    // ========== CONTENT TYPE TESTS ==========

    @Test
    @DisplayName("Should handle JSON content type")
    @WithMockUser
    void findProductById_JsonContentType() throws Exception {
        // Arrange
        when(productService.findProductById(1L)).thenReturn(testProduct);

        // Act & Assert
        mockMvc.perform(get("/api/products/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    @WithMockUser(roles = "PRODUCT_ADMIN")
    void createProduct_ServiceException_ErrorResponse() throws Exception {
        // Arrange
        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        // Act & Assert
        mockMvc.perform(post("/api/products/createProduct")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testProductRequest)))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(999))
                .andExpect(jsonPath("$.msg").value("Database connection failed"));
    }

    @Test
    @DisplayName("Should handle invalid path variables")
    @WithMockUser
    void findProductById_InvalidPathVariable() throws Exception {
        // Act & Assert - Non-numeric ID
        mockMvc.perform(get("/api/products/{id}", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
