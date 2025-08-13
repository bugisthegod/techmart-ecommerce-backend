package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.request.OrderRequest;
import com.abel.ecommerce.dto.response.OrderItemResponse;
import com.abel.ecommerce.dto.response.OrderResponse;
import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.OrderItem;
import com.abel.ecommerce.exception.OrderNotFoundException;
import com.abel.ecommerce.exception.OrderStatusException;
import com.abel.ecommerce.repository.OrderItemRepository;
import com.abel.ecommerce.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final AddressService addressService;
    private final ProductService productService;

    /**
     * Create order from cart
     * TODO: 你来实现这个核心方法 - 从购物车创建订单
     * 需要考虑：
     * 1. 验证购物车不为空
     * 2. 验证地址存在且属于用户
     * 3. 检查商品库存是否充足
     * 4. 计算订单总金额
     * 5. 扣减库存（并发安全）
     * 6. 创建订单和订单项
     * 7. 清空购物车
     * 8. 整个过程使用事务保证一致性
     */
    @Transactional
    public Order createOrder(Long userId, OrderRequest request) {
        // TODO: 你来实现订单创建的核心业务逻辑
        // 提示：这是最复杂的方法，涉及多表操作和事务管理
        
        // 1. 验证购物车
        // 2. 验证地址
        // 3. 检查库存
        // 4. 生成订单号
        // 5. 计算金额
        // 6. 创建订单
        // 7. 创建订单项
        // 8. 扣减库存
        // 9. 清空购物车
        
        throw new UnsupportedOperationException("TODO: Implement order creation logic");
    }

    /**
     * Cancel order
     * TODO: 你来实现订单取消逻辑
     * 需要考虑：
     * 1. 验证订单状态是否可以取消
     * 2. 恢复商品库存
     * 3. 更新订单状态
     * 4. 记录取消时间
     */
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        // TODO: 你来实现订单取消逻辑
        throw new UnsupportedOperationException("TODO: Implement order cancellation logic");
    }

    /**
     * Pay order (simulate payment)
     * TODO: 你来实现支付逻辑
     * 需要考虑：
     * 1. 验证订单状态
     * 2. 更新支付状态
     * 3. 记录支付时间
     */
    @Transactional
    public void payOrder(Long userId, Long orderId) {
        // TODO: 你来实现支付逻辑
        throw new UnsupportedOperationException("TODO: Implement payment logic");
    }

    /**
     * Ship order
     * TODO: 你来实现发货逻辑
     */
    @Transactional
    public void shipOrder(Long orderId) {
        // TODO: 你来实现发货逻辑
        throw new UnsupportedOperationException("TODO: Implement shipping logic");
    }

    /**
     * Complete order
     * TODO: 你来实现订单完成逻辑
     */
    @Transactional
    public void completeOrder(Long userId, Long orderId) {
        // TODO: 你来实现订单完成逻辑
        throw new UnsupportedOperationException("TODO: Implement order completion logic");
    }

    // 以下是简单的查询方法，已经实现
    
    public Page<OrderResponse> findOrdersByUserId(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return orders.map(this::convertToOrderResponse);
    }

    public Page<OrderResponse> findOrdersByUserIdAndStatus(Long userId, Integer status, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status, pageable);
        return orders.map(this::convertToOrderResponse);
    }

    public OrderResponse findOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found or doesn't belong to user"));
        return convertToOrderResponse(order);
    }

    public OrderResponse findOrderByOrderNo(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new OrderNotFoundException(orderNo, "order number"));
        return convertToOrderResponse(order);
    }

    public List<OrderItemResponse> findOrderItems(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return items.stream()
                .map(this::convertToOrderItemResponse)
                .collect(Collectors.toList());
    }

    public long countOrdersByUserId(Long userId) {
        return orderRepository.countByUserId(userId);
    }

    public long countOrdersByUserIdAndStatus(Long userId, Integer status) {
        return orderRepository.countByUserIdAndStatus(userId, status);
    }

    /**
     * Generate unique order number
     * TODO: 你可以优化这个方法，实现更好的订单号生成策略
     */
    public String generateOrderNo() {
        // Simple implementation - you can improve this
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.valueOf((int)(Math.random() * 1000));
        return "ORD" + timestamp + random;
    }

    // Helper methods for conversion
    
    private OrderResponse convertToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        BeanUtils.copyProperties(order, response);
        
        // Set computed fields
        response.setStatusText(order.getStatusText());
        response.setCanBeCancelled(order.canBeCancelled());
        response.setCanBeShipped(order.canBeShipped());
        response.setCanBeCompleted(order.canBeCompleted());
        
        // Load order items
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderItemResponse> itemResponses = items.stream()
                .map(this::convertToOrderItemResponse)
                .collect(Collectors.toList());
        response.setItems(itemResponses);
        response.setTotalItems(itemResponses.size());
        
        return response;
    }

    private OrderItemResponse convertToOrderItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        BeanUtils.copyProperties(item, response);
        return response;
    }
}
