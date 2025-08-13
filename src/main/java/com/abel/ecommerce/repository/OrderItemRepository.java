package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find order items by order ID
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Find order items by order number
     */
    List<OrderItem> findByOrderNo(String orderNo);

    /**
     * Find order items by product ID
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * Count items in an order
     */
    long countByOrderId(Long orderId);

    /**
     * Delete order items by order ID
     */
    void deleteByOrderId(Long orderId);

    /**
     * Delete order items by order number
     */
    void deleteByOrderNo(String orderNo);

    /**
     * Get total quantity for a product in orders
     */
    @Query("SELECT SUM(oi.quantity) FROM OrderItem oi WHERE oi.productId = :productId")
    Long getTotalQuantityByProductId(@Param("productId") Long productId);

    /**
     * Find order items by multiple order IDs
     */
    List<OrderItem> findByOrderIdIn(List<Long> orderIds);

    /**
     * Check if product exists in any order
     */
    boolean existsByProductId(Long productId);
}
