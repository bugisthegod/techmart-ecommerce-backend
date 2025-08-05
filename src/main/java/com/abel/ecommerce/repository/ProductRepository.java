package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find products by categoryId
    List<Product> findByCategoryId(Long categoryId);

    // Find products by status
    List<Product> findByStatus(Integer status);

    // Find products by categoryId and status
    List<Product> findByCategoryIdAndStatus(Long categoryId, Integer status);

    // Find products between price
    List<Product> findByPriceBetween(BigDecimal priceStart, BigDecimal priceEnd);

    // Find products by name containing
    List<Product> findByNameContaining(String name);

    // Find products by status order by created time desc
    List<Product> findByStatusOrderByCreatedAtDesc(Integer status);

    // Find products by status order by sales desc
    List<Product> findByStatusOrderBySalesDesc(Integer status);

    // Find products by status pageable
    List<Product> findByStatus(Integer status, Pageable pageable);

    // Find products by categoryId pageable
    List<Product> findByCategoryId(Long categoryId, Pageable pageable);

    // Count the product number by categoryId
    long countByCategoryId(Long categoryId);

    // Count the product number by status
    long countByStatus(Integer status);

}
