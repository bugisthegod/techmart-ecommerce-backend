package com.abel.ecommerce.service;

import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.OrderItem;
import com.abel.ecommerce.exception.OrderNotFoundException;
import com.abel.ecommerce.exception.OrderStatusException;
import com.abel.ecommerce.repository.OrderItemRepository;
import com.abel.ecommerce.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;




    /**
     * Pay order (simulate payment)
     */
    @Transactional
    public Order payOrder(Long userId, Long orderId) {
        // Check if order exists and belongs to user
        Order order = findOrderByIdAndUserId(orderId, userId);

        // Check if status is pending payment
        if (!order.isPendingPayment()) {
            throw new OrderStatusException("Payment failed: Order is not in pending payment status");
        }

        // Update order status and payment time
        order.setStatus(Order.STATUS_PAID);
        order.setPaymentTime(LocalDateTime.now());
        return orderRepository.save(order);
    }

    /**
     * Ship order
     */
    @Transactional
    public Order shipOrder(Long orderId) {
        // Check if order exists
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Check if status is paid
        if (!order.canBeShipped()) {
            throw new OrderStatusException("Only paid order can be shipped");
        }

        // Update order status and delivery time
        order.setStatus(Order.STATUS_SHIPPED);
        order.setDeliveryTime(LocalDateTime.now());
        return orderRepository.save(order);
    }

    /**
     * Complete order
     */
    @Transactional
    public Order completeOrder(Long userId, Long orderId) {
        // Find order and verify it belongs to user
        Order order = findOrderByIdAndUserId(orderId, userId);

        // Verify order status
        if (!order.canBeCompleted()) {
            throw OrderStatusException.cannotComplete(order.getOrderNo());
        }

        // Update order status and receive time
        order.setReceiveTime(LocalDateTime.now());
        order.setStatus(Order.STATUS_COMPLETED);
        return orderRepository.save(order);
    }

    /**
     * Cancel order
     */
    @Transactional
    public Order cancelOrder(Long userId, Long orderId) {
        // Find order and verify it belongs to user
        Order order = findOrderByIdAndUserId(orderId, userId);
        
        // Check if order can be cancelled
        if (!order.canBeCancelled()) {
            throw OrderStatusException.cannotCancel(order.getOrderNo());
        }
        
        // Update order status
        order.setStatus(Order.STATUS_CANCELLED);
        return orderRepository.save(order);
    }

    // Query methods - return Entities, not DTOs
    
    public Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
    
    public Order findOrderByIdAndUserId(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found or doesn't belong to user"));
    }

    public Page<Order> findOrdersByUserId(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public Page<Order> findOrdersByUserIdAndStatus(Long userId, Integer status, Pageable pageable) {
        return orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
    }

    public Order findOrderByOrderNo(String orderNo) {
        return orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new OrderNotFoundException(orderNo, "order number"));
    }

    public List<OrderItem> findOrderItems(Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }
    
    public List<OrderItem> findOrderItemsByOrderNo(String orderNo) {
        return orderItemRepository.findByOrderNo(orderNo);
    }

    public long countOrdersByUserId(Long userId) {
        return orderRepository.countByUserId(userId);
    }

    public long countOrdersByUserIdAndStatus(Long userId, Integer status) {
        return orderRepository.countByUserIdAndStatus(userId, status);
    }

    /**
     * Generate unique order number
     */
    public String generateOrderNo(Long userId) {
        // Format: timestamp(yyyyMMddHHmmss) + userSuffix + random
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String userSuffix = String.format("%04d", userId % 10000);
        String random = String.format("%03d", new Random().nextInt(1000));
        return timestamp + userSuffix + random;
    }
    
    /**
     * Check if order exists by order number
     */
    public boolean existsByOrderNo(String orderNo) {
        return orderRepository.existsByOrderNo(orderNo);
    }
    
    /**
     * Save order items
     */
    @Transactional
    public List<OrderItem> saveOrderItems(List<OrderItem> orderItems) {
        return orderItemRepository.saveAll(orderItems);
    }
    
    /**
     * Update order
     */
    @Transactional
    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }
}
