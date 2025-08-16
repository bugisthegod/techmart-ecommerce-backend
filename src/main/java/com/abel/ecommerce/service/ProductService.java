package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.ProductRequest;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.exception.ProductNotFoundException;
import com.abel.ecommerce.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public Product createProduct(ProductRequest request) {
        // TODO: check if category name already exists (delete category ID and create Product)

        // Create Product
        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        product.setStatus(1); // Active status

        return productRepository.save(product);
    }

    public Product updateProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(ProductRequest request, Long id) {
        Product existingProduct = findProductById(id);
        BeanUtils.copyProperties(request, existingProduct);
        return productRepository.save(existingProduct);
    }

    public void deleteProduct(Long id) {
        findProductById(id); // Check if product exists
        productRepository.deleteById(id);
    }

    public List<Product> findProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public List<Product> searchProducts(String name) {
        return productRepository.findByNameContaining(name);
    }

    public Page<Product> findProductsWithPagination(Long categoryId, Integer status, Pageable pageable) {
        if (categoryId != null && status != null) return productRepository.findByCategoryIdAndStatus(categoryId, status, pageable);
        else if (categoryId != null) return productRepository.findByCategoryId(categoryId, pageable);
        else if (status != null) return productRepository.findByStatus(status, pageable);
        else return productRepository.findAll(pageable);

    }

    public Product findProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id, "ID"));
    }

}
