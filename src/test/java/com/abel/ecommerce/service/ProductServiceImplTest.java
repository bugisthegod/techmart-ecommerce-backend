package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.ProductRequest;
import com.abel.ecommerce.entity.CartItem;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.exception.InsufficientStockException;
import com.abel.ecommerce.exception.ProductNotFoundException;
import com.abel.ecommerce.repository.ProductRepository;
import com.abel.ecommerce.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductServiceImpl
 *
 * Key concepts demonstrated:
 * - @ExtendWith(MockitoExtension.class): Enables Mockito annotations
 * - @Mock: Creates mock objects (dependencies)
 * - @InjectMocks: Creates instance and injects mocks into it
 * - @BeforeEach: Runs before each test method
 * - @Test: Marks a test method
 * - @DisplayName: Provides readable test name
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private RedisTemplate<String, Object> objectRedisTemplate;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductRequest testProductRequest;

    /**
     * Setup method - runs before each test
     * Creates common test data to avoid duplication
     */
    @BeforeEach
    void setUp() {
        // Create a test product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Test Product");
        testProduct.setCategoryId(10L);
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setOriginalPrice(new BigDecimal("129.99"));
        testProduct.setDescription("Test Description");
        testProduct.setMainImage("test-image.jpg");
        testProduct.setStock(100);
        testProduct.setSales(0);
        testProduct.setStatus(Product.ACTIVE_PRODUCT);

        // Create a test product request
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
    @DisplayName("Should create product successfully")
    void createProduct_Success() {
        // Arrange (Given) - Setup test data and mock behavior
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act (When) - Execute the method under test
        Product result = productService.createProduct(testProductRequest);

        // Assert (Then) - Verify the results
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getStatus()).isEqualTo(Product.ACTIVE_PRODUCT);

        // Verify that repository.save was called exactly once
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should set product status to active when creating")
    void createProduct_ShouldSetActiveStatus() {
        // Arrange
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product savedProduct = invocation.getArgument(0);
            savedProduct.setId(1L);
            return savedProduct;
        });

        // Act
        Product result = productService.createProduct(testProductRequest);

        // Assert
        assertThat(result.getStatus()).isEqualTo(Product.ACTIVE_PRODUCT);
    }

    // ========== FIND PRODUCT TESTS ==========

    @Test
    @DisplayName("Should find product by ID successfully")
    void findProductById_Success() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act
        Product result = productService.findProductById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Product");

        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when product not found")
    void findProductById_NotFound_ThrowsException() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert - Use assertThatThrownBy for exception testing
        assertThatThrownBy(() -> productService.findProductById(999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("999");

        verify(productRepository, times(1)).findById(999L);
    }

    // ========== UPDATE PRODUCT TESTS ==========

    @Test
    @DisplayName("Should update product successfully")
    void updateProduct_Success() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        Product result = productService.updateProduct(testProductRequest, 1L);

        // Assert
        assertThat(result).isNotNull();
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw exception when updating non-existent product")
    void updateProduct_NotFound_ThrowsException() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct(testProductRequest, 999L))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).save(any(Product.class));
    }

    // ========== DELETE PRODUCT TESTS ==========

    @Test
    @DisplayName("Should delete product successfully")
    void deleteProduct_Success() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        doNothing().when(productRepository).deleteById(1L);

        // Act
        productService.deleteProduct(1L);

        // Assert
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent product")
    void deleteProduct_NotFound_ThrowsException() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(ProductNotFoundException.class);

        verify(productRepository, times(1)).findById(999L);
        verify(productRepository, never()).deleteById(anyLong());
    }

    // ========== SEARCH AND FILTER TESTS ==========

    @Test
    @DisplayName("Should find products by category")
    void findProductsByCategory_Success() {
        // Arrange
        List<Product> products = Arrays.asList(testProduct);
        when(productRepository.findByCategoryId(10L)).thenReturn(products);

        // Act
        List<Product> result = productService.findProductsByCategory(10L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategoryId()).isEqualTo(10L);
        verify(productRepository, times(1)).findByCategoryId(10L);
    }

    @Test
    @DisplayName("Should search products by name")
    void searchProducts_Success() {
        // Arrange
        List<Product> products = Arrays.asList(testProduct);
        when(productRepository.findByNameContaining("Test")).thenReturn(products);

        // Act
        List<Product> result = productService.searchProducts("Test");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).contains("Test");
        verify(productRepository, times(1)).findByNameContaining("Test");
    }

    @Test
    @DisplayName("Should find products with pagination - all filters")
    void findProductsWithPagination_WithAllFilters() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productRepository.findByCategoryIdAndStatus(10L, 1, pageable))
                .thenReturn(productPage);

        // Act
        Page<Product> result = productService.findProductsWithPagination(10L, 1, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(productRepository, times(1)).findByCategoryIdAndStatus(10L, 1, pageable);
    }

    @Test
    @DisplayName("Should find products with pagination - category only")
    void findProductsWithPagination_CategoryOnly() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productRepository.findByCategoryId(10L, pageable)).thenReturn(productPage);

        // Act
        Page<Product> result = productService.findProductsWithPagination(10L, null, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository, times(1)).findByCategoryId(10L, pageable);
    }

    @Test
    @DisplayName("Should find products with pagination - status only")
    void findProductsWithPagination_StatusOnly() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productRepository.findByStatus(1, pageable)).thenReturn(productPage);

        // Act
        Page<Product> result = productService.findProductsWithPagination(null, 1, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository, times(1)).findByStatus(1, pageable);
    }

    @Test
    @DisplayName("Should find all products with pagination - no filters")
    void findProductsWithPagination_NoFilters() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(Arrays.asList(testProduct));
        when(productRepository.findAll(pageable)).thenReturn(productPage);

        // Act
        Page<Product> result = productService.findProductsWithPagination(null, null, pageable);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(productRepository, times(1)).findAll(pageable);
    }

    // ========== RESERVE PRODUCTS FOR ORDER TESTS ==========

    @Test
    @DisplayName("Should reserve products successfully")
    void reserveProductsForOrder_Success() {
        // Arrange
        CartItem cartItem = new CartItem();
        cartItem.setProductId(1L);
        cartItem.setQuantity(10);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(testProduct);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        List<Product> result = productService.reserveProductsForOrder(Arrays.asList(cartItem));

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStock()).isEqualTo(90); // 100 - 10
        assertThat(result.get(0).getSales()).isEqualTo(10); // 0 + 10

        verify(productRepository, times(1)).findByIdForUpdate(1L);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when stock is not enough")
    void reserveProductsForOrder_InsufficientStock_ThrowsException() {
        // Arrange
        CartItem cartItem = new CartItem();
        cartItem.setProductId(1L);
        cartItem.setQuantity(150); // More than available stock (100)

        when(productRepository.findByIdForUpdate(1L)).thenReturn(testProduct);

        // Act & Assert
        assertThatThrownBy(() -> productService.reserveProductsForOrder(Arrays.asList(cartItem)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Test Product")
                .hasMessageContaining("100")
                .hasMessageContaining("150");

        verify(productRepository, times(1)).findByIdForUpdate(1L);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should reserve multiple products successfully")
    void reserveProductsForOrder_MultipleProducts_Success() {
        // Arrange
        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("Product 2");
        product2.setStock(50);
        product2.setSales(0);

        CartItem cartItem1 = new CartItem();
        cartItem1.setProductId(1L);
        cartItem1.setQuantity(10);

        CartItem cartItem2 = new CartItem();
        cartItem2.setProductId(2L);
        cartItem2.setQuantity(5);

        when(productRepository.findByIdForUpdate(1L)).thenReturn(testProduct);
        when(productRepository.findByIdForUpdate(2L)).thenReturn(product2);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        List<Product> result = productService.reserveProductsForOrder(Arrays.asList(cartItem1, cartItem2));

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStock()).isEqualTo(90);
        assertThat(result.get(0).getSales()).isEqualTo(10);
        assertThat(result.get(1).getStock()).isEqualTo(45);
        assertThat(result.get(1).getSales()).isEqualTo(5);

        verify(productRepository, times(2)).findByIdForUpdate(anyLong());
        verify(productRepository, times(2)).save(any(Product.class));
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    @DisplayName("Should handle exact stock quantity reservation")
    void reserveProductsForOrder_ExactStock_Success() {
        // Arrange
        CartItem cartItem = new CartItem();
        cartItem.setProductId(1L);
        cartItem.setQuantity(100); // Exact stock amount

        when(productRepository.findByIdForUpdate(1L)).thenReturn(testProduct);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        List<Product> result = productService.reserveProductsForOrder(Arrays.asList(cartItem));

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStock()).isEqualTo(0);
        assertThat(result.get(0).getSales()).isEqualTo(100);
    }

    @Test
    @DisplayName("Should return empty list when no products provided")
    void findProductsByCategory_EmptyResult() {
        // Arrange
        when(productRepository.findByCategoryId(999L)).thenReturn(Arrays.asList());

        // Act
        List<Product> result = productService.findProductsByCategory(999L);

        // Assert
        assertThat(result).isEmpty();
        verify(productRepository, times(1)).findByCategoryId(999L);
    }
}
