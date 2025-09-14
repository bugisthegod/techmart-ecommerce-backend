package com.abel.ecommerce.facade;

import com.abel.ecommerce.dto.request.OrderRequest;
import com.abel.ecommerce.entity.*;
import com.abel.ecommerce.exception.*;
import com.abel.ecommerce.service.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final CartService cartService;
    private final AddressService addressService;
    private final ProductService productService;
    private final OrderService orderService;

    private final StringRedisTemplate redisTemplate;

    /**
     * Create order from cart
     * This is the most complex business logic involving multiple tables and transactions
     */
    @Transactional
    public Order createOrder(Long userId, OrderRequest request) {



        // Now only focus on create order
        // Check is there any selected cart items
        List<CartItem> selectedCartItems = cartService.getSelectedCartItems(userId);
        if (selectedCartItems.isEmpty()) throw new OrderException("Please select items to order");

        // Check address belongs to user
        Address address = addressService.findAddressByIdAndUserId(request.getAddressId(), userId);

        // Check if product stock is enough and calculate total price
        // Reserve all products for order
        List<Product> products = productService.reserveProductsForOrder(selectedCartItems);

        Map<Long, Product> productMap = products.stream().collect(Collectors.toMap(Product::getId, p -> p));

        BigDecimal freightAmount = new BigDecimal("10.00");
        BigDecimal totalAmount = selectedCartItems.stream().map(cartItem -> {
            Product product = productMap.get(cartItem.getProductId());
            return product.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
        }).reduce(BigDecimal.ZERO, BigDecimal::add);

        totalAmount = totalAmount.add(freightAmount);

        // generate unique orderNo
        String newOrderNo = orderService.generateOrderNo(userId);

        // Create order
        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo(newOrderNo);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount); // Pay amount equals total amount (no discounts applied)
        order.setFreightAmount(freightAmount);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverAddress(address.getFullAddress());
        order.setReceiverPhone(address.getReceiverPhone());
        order.setStatus(Order.STATUS_PENDING_PAYMENT);
        orderService.updateOrder(order);
        List<OrderItem> orderItems = selectedCartItems.stream().map(cartItem -> {
            Product product = productMap.get(cartItem.getProductId());
            BigDecimal itemAmount = product.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setOrderNo(newOrderNo);
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setProductPrice(product.getPrice());
            orderItem.setTotalAmount(itemAmount);
            return orderItem;
        }).toList();

        // Save all order items to database
        orderService.saveOrderItems(orderItems);

        // Remove all selected cart items
        for (CartItem cartItem : selectedCartItems) {
            cartService.removeFromCart(userId, cartItem.getId());
        }

        return order;
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
