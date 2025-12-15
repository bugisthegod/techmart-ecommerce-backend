package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for ProductRepository
 *
 * Key concepts demonstrated:
 * - @DataJpaTest: Sets up an in-memory database for testing (H2)
 * - @Autowired: Injects the real repository (not mocked)
 * - Tests actual database operations
 * - Transactions are rolled back after each test automatically
 *
 * Note: @DataJpaTest uses H2 in-memory database by default, so you don't need MySQL running
 */
@DataJpaTest
@DisplayName("ProductRepository Integration Tests")
@org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase(replace = org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    private Product testProduct1;
    private Product testProduct2;
    private Product testProduct3;

    /**
     * Setup method - creates test data in the database before each test
     */
    @BeforeEach
    void setUp() {
        // Clear any existing data
        productRepository.deleteAll();

        // Create test products
        testProduct1 = new Product();
        testProduct1.setName("Laptop");
        testProduct1.setCategoryId(1L);
        testProduct1.setPrice(new BigDecimal("999.99"));
        testProduct1.setOriginalPrice(new BigDecimal("1299.99"));
        testProduct1.setDescription("High-performance laptop");
        testProduct1.setMainImage("laptop.jpg");
        testProduct1.setStock(50);
        testProduct1.setSales(10);
        testProduct1.setStatus(Product.ACTIVE_PRODUCT);

        testProduct2 = new Product();
        testProduct2.setName("Mouse");
        testProduct2.setCategoryId(1L);
        testProduct2.setPrice(new BigDecimal("29.99"));
        testProduct2.setOriginalPrice(new BigDecimal("39.99"));
        testProduct2.setDescription("Wireless mouse");
        testProduct2.setMainImage("mouse.jpg");
        testProduct2.setStock(100);
        testProduct2.setSales(25);
        testProduct2.setStatus(Product.ACTIVE_PRODUCT);

        testProduct3 = new Product();
        testProduct3.setName("Keyboard");
        testProduct3.setCategoryId(2L);
        testProduct3.setPrice(new BigDecimal("79.99"));
        testProduct3.setOriginalPrice(new BigDecimal("99.99"));
        testProduct3.setDescription("Mechanical keyboard");
        testProduct3.setMainImage("keyboard.jpg");
        testProduct3.setStock(0); // Out of stock
        testProduct3.setSales(50);
        testProduct3.setStatus(Product.NONACTIVE_PRODUCT);

        // Save test products to database
        productRepository.save(testProduct1);
        productRepository.save(testProduct2);
        productRepository.save(testProduct3);
    }

    // ========== CUSTOM QUERY TESTS ==========

    @Test
    @DisplayName("Should find products by category ID")
    void findByCategoryId() {
        // Act
        List<Product> category1Products = productRepository.findByCategoryId(1L);
        List<Product> category2Products = productRepository.findByCategoryId(2L);

        // Assert
        assertThat(category1Products).hasSize(2);
        assertThat(category1Products).extracting(Product::getName)
                .containsExactlyInAnyOrder("Laptop", "Mouse");

        assertThat(category2Products).hasSize(1);
        assertThat(category2Products.get(0).getName()).isEqualTo("Keyboard");
    }

    @Test
    @DisplayName("Should find products by status")
    void findByStatus() {
        // Act
        List<Product> activeProducts = productRepository.findByStatus(Product.ACTIVE_PRODUCT);
        List<Product> inactiveProducts = productRepository.findByStatus(Product.NONACTIVE_PRODUCT);

        // Assert
        assertThat(activeProducts).hasSize(2);
        assertThat(activeProducts).extracting(Product::getName)
                .containsExactlyInAnyOrder("Laptop", "Mouse");
        assertThat(inactiveProducts).hasSize(1);
        assertThat(inactiveProducts.get(0).getName()).isEqualTo("Keyboard");
    }

    @Test
    @DisplayName("Should find products by name containing")
    void findByNameContaining() {
        // Act
        List<Product> results = productRepository.findByNameContaining("o");

        // Assert - Should find "Laptop", "Mouse", "Keyboard" (all contain 'o')
        assertThat(results).hasSize(3);
    }

    @Test
    @DisplayName("Should find products by price range")
    void findByPriceBetween() {
        // Act
        List<Product> results = productRepository.findByPriceBetween(
                new BigDecimal("20.00"),
                new BigDecimal("100.00")
        );

        // Assert - Should find Mouse (29.99) and Keyboard (79.99)
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Product::getName)
                .containsExactlyInAnyOrder("Mouse", "Keyboard");
    }

    // ========== PAGINATION TESTS ==========

    @Test
    @DisplayName("Should find products by category with pagination")
    void findByCategoryId_WithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> page = productRepository.findByCategoryId(1L, pageable);

        // Assert
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should find products by status with pagination")
    void findByStatus_WithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> page = productRepository.findByStatus(Product.ACTIVE_PRODUCT, pageable);

        // Assert
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("Should find products by category and status with pagination")
    void findByCategoryIdAndStatus_WithPagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Act
        Page<Product> page = productRepository.findByCategoryIdAndStatus(
                1L,
                Product.ACTIVE_PRODUCT,
                pageable
        );

        // Assert
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allMatch(p -> p.getCategoryId().equals(1L));
        assertThat(page.getContent()).allMatch(p -> p.getStatus().equals(Product.ACTIVE_PRODUCT));
    }

    // ========== SORTING TESTS ==========

    @Test
    @DisplayName("Should find products ordered by created time desc")
    void findByStatusOrderByCreatedAtDesc() {
        // Act
        List<Product> results = productRepository.findByStatusOrderByCreatedAtDesc(Product.ACTIVE_PRODUCT);

        // Assert
        assertThat(results).hasSize(2);
        // Note: Order depends on when products were created in setUp
    }

    @Test
    @DisplayName("Should find products ordered by sales desc")
    void findByStatusOrderBySalesDesc() {
        // Act
        List<Product> results = productRepository.findByStatusOrderBySalesDesc(Product.ACTIVE_PRODUCT);

        // Assert
        assertThat(results).hasSize(2);
        // First product should have higher sales
        assertThat(results.get(0).getSales()).isGreaterThanOrEqualTo(results.get(1).getSales());
    }

    // ========== COUNT TESTS ==========

    @Test
    @DisplayName("Should count products by category")
    void countByCategoryId() {
        // Act
        long category1Count = productRepository.countByCategoryId(1L);
        long category2Count = productRepository.countByCategoryId(2L);
        long category3Count = productRepository.countByCategoryId(3L);

        // Assert
        assertThat(category1Count).isEqualTo(2);
        assertThat(category2Count).isEqualTo(1);
        assertThat(category3Count).isEqualTo(0);
    }

    @Test
    @DisplayName("Should count products by status")
    void countByStatus() {
        // Act
        long activeCount = productRepository.countByStatus(Product.ACTIVE_PRODUCT);
        long inactiveCount = productRepository.countByStatus(Product.NONACTIVE_PRODUCT);

        // Assert
        assertThat(activeCount).isEqualTo(2);
        assertThat(inactiveCount).isEqualTo(1);
    }

    // ========== CUSTOM @Query TESTS ==========

    @Test
    @DisplayName("Should find product by ID for update")
    void findByIdForUpdate() {
        // Act
        Product result = productRepository.findByIdForUpdate(testProduct1.getId());

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testProduct1.getId());
        assertThat(result.getName()).isEqualTo("Laptop");
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("Should handle empty result for non-existent category")
    void findByCategoryId_EmptyResult() {
        // Act
        List<Product> results = productRepository.findByCategoryId(999L);

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty search results")
    void findByNameContaining_EmptyResult() {
        // Act
        List<Product> results = productRepository.findByNameContaining("NonExistentProduct");

        // Assert
        assertThat(results).isEmpty();
    }
}
