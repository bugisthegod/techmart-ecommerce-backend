package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.ProductRequest;
import com.abel.ecommerce.entity.CartItem;
import com.abel.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    /**
     * Create a new product
     * @param request Product creation request
     * @return Created product
     */
    Product createProduct(ProductRequest request);

    /**
     * Update an existing product entity
     * @param product Product entity to update
     */
    void updateProduct(Product product);

    /**
     * Update a product by ID
     * @param request Product update request
     * @param id Product ID
     * @return Updated product
     */
    Product updateProduct(ProductRequest request, Long id);

    /**
     * Delete a product by ID
     * @param id Product ID
     */
    void deleteProduct(Long id);

    /**
     * Find products by category ID
     * @param categoryId Category ID
     * @return List of products
     */
    List<Product> findProductsByCategory(Long categoryId);

    /**
     * Search products by name
     * @param name Product name or partial name
     * @return List of matching products
     */
    List<Product> searchProducts(String name);

    /**
     * Find products with pagination and filters
     * @param categoryId Category ID filter (optional)
     * @param status Status filter (optional)
     * @param pageable Pagination parameters
     * @return Page of products
     */
    Page<Product> findProductsWithPagination(Long categoryId, Integer status, Pageable pageable);

    /**
     * Find product by ID
     * @param id Product ID
     * @return Product entity
     */
    Product findProductById(Long id);

    /**
     * Reserve products for order (lock rows and update stock)
     * @param cartItems Cart items to reserve products for
     * @return List of reserved products
     */
    List<Product> reserveProductsForOrder(List<CartItem> cartItems);
}
