package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find products by categoryId
    List<Product> findByCategoryId(Long categoryId);

    // Find products by status
    List<Product> findByStatus(Integer status);

    // Find products by categoryId and status
    Page<Product> findByCategoryIdAndStatus(Long categoryId, Integer status, Pageable pageable);

    // Find products between price
    List<Product> findByPriceBetween(BigDecimal priceStart, BigDecimal priceEnd);

    // Find products by name containing
    List<Product> findByNameContaining(String name);

    // Find products by status order by created time desc
    List<Product> findByStatusOrderByCreatedAtDesc(Integer status);

    // Find products by status order by sales desc
    List<Product> findByStatusOrderBySalesDesc(Integer status);

    // Find products by status pageable
    Page<Product> findByStatus(Integer status, Pageable pageable);

    // Find products by categoryId pageable
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    // Count the product number by categoryId
    long countByCategoryId(Long categoryId);

    // Count the product number by status
    long countByStatus(Integer status);

    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Product findByIdForUpdate(@Param("id") Long id);

}
