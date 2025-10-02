package com.abel.ecommerce.service.impl;

import com.abel.ecommerce.dto.request.ProductRequest;
import com.abel.ecommerce.entity.CartItem;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.exception.InsufficientStockException;
import com.abel.ecommerce.exception.ProductNotFoundException;
import com.abel.ecommerce.repository.ProductRepository;
import com.abel.ecommerce.service.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public Product createProduct(ProductRequest request) {
        // TODO: check if category name already exists (delete category ID and create Product)

        // Create Product
        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        product.setStatus(1); // Active status

        return productRepository.save(product);
    }

    @Override
    public void updateProduct(Product product) {
        productRepository.save(product);
    }

    @Override
    @Transactional
    public Product updateProduct(ProductRequest request, Long id) {
        Product existingProduct = findProductById(id);
        BeanUtils.copyProperties(request, existingProduct);
        return productRepository.save(existingProduct);
    }

    @Override
    public void deleteProduct(Long id) {
        findProductById(id); // Check if product exists
        productRepository.deleteById(id);
    }

    @Override
    public List<Product> findProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    @Override
    public List<Product> searchProducts(String name) {
        return productRepository.findByNameContaining(name);
    }

    @Override
    public Page<Product> findProductsWithPagination(Long categoryId, Integer status, Pageable pageable) {
        if (categoryId != null && status != null) return productRepository.findByCategoryIdAndStatus(categoryId, status, pageable);
        else if (categoryId != null) return productRepository.findByCategoryId(categoryId, pageable);
        else if (status != null) return productRepository.findByStatus(status, pageable);
        else return productRepository.findAll(pageable);

    }

    @Override
    public Product findProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id, "ID"));
    }

    @Override
    public List<Product> reserveProductsForOrder(List<CartItem> cartItems) {
        List<Product> products = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            // lock row
            Product product = productRepository.findByIdForUpdate(cartItem.getProductId());
            if (cartItem.getQuantity() > product.getStock())
                throw new InsufficientStockException(product.getName(), product.getStock(), cartItem.getQuantity());
            product.setStock(product.getStock() - cartItem.getQuantity());
            product.setSales(product.getSales() + cartItem.getQuantity());
            productRepository.save(product);
            products.add(product);
        }
        return products;
    }

}
