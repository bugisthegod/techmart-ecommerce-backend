package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find order by order number
     */
    Optional<Order> findByOrderNo(String orderNo);

    /**
     * Check if order number exists
     */
    boolean existsByOrderNo(String orderNo);

    /**
     * Find orders by user ID with pagination
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find orders by user ID and status
     */
    Page<Order> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, Integer status, Pageable pageable);

    /**
     * Find orders by status
     */
    List<Order> findByStatus(Integer status);

    /**
     * Find orders by user ID and status (list)
     */
    List<Order> findByUserIdAndStatus(Long userId, Integer status);

    /**
     * Count orders by user ID
     */
    long countByUserId(Long userId);

    /**
     * Count orders by user ID and status
     */
    long countByUserIdAndStatus(Long userId, Integer status);

    /**
     * Find orders by user ID and time range
     */
    @Query("SELECT o FROM Order o WHERE o.userId = :userId AND o.createdAt BETWEEN :startTime AND :endTime ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndTimeRange(@Param("userId") Long userId, 
                                       @Param("startTime") LocalDateTime startTime, 
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Find orders by multiple statuses
     */
    List<Order> findByStatusInOrderByCreatedAtDesc(List<Integer> statuses);

    /**
     * Find user's order by order ID (security check)
     */
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /**
     * Check if order belongs to user
     */
    boolean existsByIdAndUserId(Long id, Long userId);

    /**
     * Find orders that need to be auto-cancelled (pending payment for too long)
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt < :expireTime")
    List<Order> findExpiredOrders(@Param("status") Integer status, @Param("expireTime") LocalDateTime expireTime);

    /**
     * Get order statistics by user
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId AND o.status = :status")
    long getOrderCountByUserAndStatus(@Param("userId") Long userId, @Param("status") Integer status);
}
