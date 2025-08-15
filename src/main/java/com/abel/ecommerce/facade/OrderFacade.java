package com.abel.ecommerce.facade;

import com.abel.ecommerce.dto.request.OrderRequest;
import com.abel.ecommerce.entity.*;
import com.abel.ecommerce.exception.*;
import com.abel.ecommerce.service.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final CartService cartService;
    private final AddressService addressService;
    private final ProductService productService;
    private final OrderService orderService;

    /**
     * Create order from cart
     * This is the most complex business logic involving multiple tables and transactions
     */
    @Transactional
    public Order createOrder(Long userId, OrderRequest request) {

    }

    /**
     * Cancel order and restore stock
     */
    @Transactional
    public void cancelOrder(Long userId, Long orderId) {
        // 1. Verify order exists and can be cancelled
        Order order = orderService.findOrderByIdAndUserId(orderId, userId);
        if (!order.canBeCancelled()) {
            throw OrderStatusException.cannotCancel(order.getOrderNo());
        }

        // 2. Get order items
        List<OrderItem> orderItems = orderService.findOrderItems(orderId);

        // 3. Restore stock for each product
        for (OrderItem orderItem : orderItems) {
            Product product = productService.findProductById(orderItem.getProductId());

            // Restore stock
            product.setStock(product.getStock() + orderItem.getQuantity());
            // Reduce sales count
            product.setSales(product.getSales() - orderItem.getQuantity());

            productService.updateProduct(product);
        }

        // 4. Update order status to cancelled
        order.setStatus(Order.STATUS_CANCELLED);
        orderService.updateOrder(order);
    }


}
