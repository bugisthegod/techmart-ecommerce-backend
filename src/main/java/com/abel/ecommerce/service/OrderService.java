package com.abel.ecommerce.service;

import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {

    /**
     * Pay order (simulate payment)
     * @param userId User ID
     * @param orderId Order ID
     * @return Updated order
     */
    Order payOrder(Long userId, Long orderId);

    /**
     * Ship order
     * @param orderId Order ID
     * @return Updated order
     */
    Order shipOrder(Long orderId);

    /**
     * Complete order
     * @param userId User ID
     * @param orderId Order ID
     * @return Updated order
     */
    Order completeOrder(Long userId, Long orderId);

    /**
     * Cancel order
     * @param userId User ID
     * @param orderId Order ID
     * @return Updated order
     */
    Order cancelOrder(Long userId, Long orderId);

    /**
     * Find order by ID
     * @param orderId Order ID
     * @return Order entity
     */
    Order findOrderById(Long orderId);

    /**
     * Find order by ID and user ID
     * @param orderId Order ID
     * @param userId User ID
     * @return Order entity
     */
    Order findOrderByIdAndUserId(Long orderId, Long userId);

    /**
     * Find orders by user ID with pagination
     * @param userId User ID
     * @param pageable Pagination parameters
     * @return Page of orders
     */
    Page<Order> findOrdersByUserId(Long userId, Pageable pageable);

    /**
     * Find orders by user ID and status with pagination
     * @param userId User ID
     * @param status Order status
     * @param pageable Pagination parameters
     * @return Page of orders
     */
    Page<Order> findOrdersByUserIdAndStatus(Long userId, Integer status, Pageable pageable);

    /**
     * Find order by order number
     * @param orderNo Order number
     * @return Order entity
     */
    Order findOrderByOrderNo(String orderNo);

    /**
     * Find order items by order ID
     * @param orderId Order ID
     * @return List of order items
     */
    List<OrderItem> findOrderItems(Long orderId);

    /**
     * Find order items by order number
     * @param orderNo Order number
     * @return List of order items
     */
    List<OrderItem> findOrderItemsByOrderNo(String orderNo);

    /**
     * Count orders by user ID
     * @param userId User ID
     * @return Number of orders
     */
    long countOrdersByUserId(Long userId);

    /**
     * Count orders by user ID and status
     * @param userId User ID
     * @param status Order status
     * @return Number of orders
     */
    long countOrdersByUserIdAndStatus(Long userId, Integer status);

    /**
     * Generate unique order number
     * @param userId User ID
     * @return Generated order number
     */
    String generateOrderNo(Long userId);

    /**
     * Generate order token for idempotency
     * @param userId User ID
     * @return Generated order token
     */
    String generateOrderToken(Long userId);

    /**
     * Validate and delete order token
     * @param token Order token
     * @return true if token was valid and deleted
     */
    boolean validateAndDeleteOrderToken(String token);

    /**
     * Check if order exists by order number
     * @param orderNo Order number
     * @return true if order exists
     */
    boolean existsByOrderNo(String orderNo);

    /**
     * Save order items
     * @param orderItems List of order items
     * @return List of saved order items
     */
    List<OrderItem> saveOrderItems(List<OrderItem> orderItems);

    /**
     * Update order
     * @param order Order entity
     * @return Updated order
     */
    Order updateOrder(Order order);
}
