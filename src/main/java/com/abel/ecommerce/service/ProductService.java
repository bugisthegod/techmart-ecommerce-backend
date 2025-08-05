package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.ProductRequest;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.exception.ProductNotFoundException;
import com.abel.ecommerce.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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

    /**
     * TODO:updateProduct() - 更新商品
     * deleteProduct() - 删除商品
     * getProductById() - 根据ID查询
     * getProductsByCategory() - 分类查询
     * searchProducts() - 搜索商品
     * getProductsWithPagination() - 分页查询
     * updateStock() - 更新库存
     */

    @Transactional
    public Product updateProduct(ProductRequest request, Long id) {

        Product exsitingProduct = findProductById(id);
        BeanUtils.copyProperties(request, exsitingProduct);
        return productRepository.save(exsitingProduct);
    }


    public Product findProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id, "ID"))
    }

}
