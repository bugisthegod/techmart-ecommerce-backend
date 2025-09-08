package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.ProductRequest;
import com.abel.ecommerce.dto.response.ProductResponse;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.exception.ProductNotFoundException;
import com.abel.ecommerce.service.ProductService;
import com.abel.ecommerce.utils.ResponseResult;
import com.abel.ecommerce.utils.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Product Management")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create new product")
    @PreAuthorize("hasRole('PRODUCT_ADMIN') or hasRole('SUPER_ADMIN')")
    @PostMapping("/createProduct")
    public ResponseResult<String> createProduct(@Valid @RequestBody ProductRequest request) {
        try {

            Product product = productService.createProduct(request);
            return ResponseResult.ok("Product created successfully with ID: " + product.getId());
        }
        catch (Exception e) {
            log.error("Failed to create product", e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Update the product")
    @PutMapping("/{id}")
    public ResponseResult<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {

        try {
            Product product = productService.updateProduct(request, id);
            ProductResponse productResponse = new ProductResponse();
            BeanUtils.copyProperties(product, productResponse);
            return ResponseResult.ok(productResponse);
        }
        catch (ProductNotFoundException e) {
            log.error("Product not found for update - ID: {}", id, e);
            return ResponseResult.error(ResultCode.PRODUCT_NOT_EXIST.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Unexpected error updating product with ID: {}", id, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Delete the product by ID")
    @DeleteMapping("/{id}")
    public ResponseResult<String> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);
            return ResponseResult.ok(ResultCode.SUCCESS);
        }
        catch (ProductNotFoundException e) {
            log.error("Product not found for deletion - ID: {}", id, e);
            return ResponseResult.error(ResultCode.PRODUCT_NOT_EXIST.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Unexpected error deleting product with ID: {}", id, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Find product by ID")
    @GetMapping("/{id}")
    public ResponseResult<ProductResponse> findProductById(@PathVariable Long id) {
        try {
            Product productById = productService.findProductById(id);
            ProductResponse productResponse = new ProductResponse();
            BeanUtils.copyProperties(productById, productResponse);
            return ResponseResult.ok(productResponse);
        }
        catch (ProductNotFoundException e) {
            log.error("Product not found - ID: {}", id, e);
            return ResponseResult.error(ResultCode.PRODUCT_NOT_EXIST.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Unexpected error finding product with ID: {}", id, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Find products with pagination")
    @GetMapping
    public ResponseResult<Page<ProductResponse>> findProductsWithPagination(@RequestParam(required = false) Long categoryId,
                                                                            @RequestParam(defaultValue = "1") Integer status,
                                                                            @RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size,
                                                                            @RequestParam(defaultValue = "createdAt") String sortBy,
                                                                            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
            Page<Product> productsWithPagination = productService.findProductsWithPagination(categoryId, status, pageable);
            Page<ProductResponse> productResponses = productsWithPagination.map(product -> {
                ProductResponse response = new ProductResponse();
                BeanUtils.copyProperties(product, response);
                return response;
            });
            return ResponseResult.ok(productResponses);
        }
        catch (RuntimeException e) {
            log.error("Runtime error finding products with pagination - categoryId: {}, status: {}, page: {}", categoryId, status, page, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Unexpected error finding products with pagination - categoryId: {}, status: {}, page: {}", categoryId, status, page, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Search products by Name")
    @GetMapping("/search")
    public ResponseResult<List<ProductResponse>> searchProducts(@PathVariable String name) {
        try {
            List<Product> products = productService.searchProducts(name);
            List<ProductResponse> productResponses = new ArrayList<>();
            if (products != null && !products.isEmpty() && products.stream().noneMatch(Objects::isNull)) {
                for (Product product : products) {
                    ProductResponse productResponse = new ProductResponse();
                    BeanUtils.copyProperties(product, productResponse);
                    productResponses.add(productResponse);
                }
                return ResponseResult.ok(productResponses);
            }
            else {
                return ResponseResult.ok(ResultCode.SUCCESS);
            }

        }
        catch (Exception e) {
            log.error("Failed to search products by name: {}", name, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

}
