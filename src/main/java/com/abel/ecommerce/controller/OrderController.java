package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.OrderRequest;
import com.abel.ecommerce.dto.response.OrderItemResponse;
import com.abel.ecommerce.dto.response.OrderResponse;
import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.OrderItem;
import com.abel.ecommerce.exception.OrderNotFoundException;
import com.abel.ecommerce.exception.OrderStatusException;
import com.abel.ecommerce.facade.OrderFacade;
import com.abel.ecommerce.service.OrderService;
import com.abel.ecommerce.utils.ResponseResult;
import com.abel.ecommerce.utils.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Order Management", description = "Order creation, payment, shipping and tracking")
public class OrderController {

    private final OrderService orderService;
    private final OrderFacade orderFacade;

    @Operation(summary = "Create order from cart", description = "Create a new order from user's selected cart items")
    @PostMapping
    public ResponseResult<OrderResponse> createOrder(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order data") @Valid @RequestBody OrderRequest request,
            HttpServletRequest httpRequest) {
        try {

            // 1. Get order idempotency token from request header
            String orderToken = httpRequest.getHeader("Idempotency-Token");
            if (StringUtils.isEmpty(orderToken)) return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), "Missing idempotency token");

            // 2. Validate if idempotency token exists
            if (!orderService.validateAndDeleteOrderToken(orderToken))
                return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), "Order already submitted " +
                        "or invalid token");

            // 3. Use facade for complex business logic
            Order order = orderFacade.createOrder(userId, request);

            // Convert to response DTO in controller
            OrderResponse response = convertToOrderResponse(order);
            return ResponseResult.ok(response);
        }
        catch (OrderNotFoundException e) {
            log.error("Order cannot be found", e);
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        }
        catch (Exception e) {
            log.error("Exception: ", e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Get user orders", description = "Get paginated list of user's orders")
    @GetMapping
    public ResponseResult<Page<OrderResponse>> getUserOrders(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Order status filter") @RequestParam(required = false) Integer status) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Order> orders;

            // Get entities from service
            if (status != null) {
                orders = orderService.findOrdersByUserIdAndStatus(userId, status, pageable);
            }
            else {
                orders = orderService.findOrdersByUserId(userId, pageable);
            }

            // Convert to response DTOs in controller
            Page<OrderResponse> orderResponses = orders.map(this::convertToOrderResponse);
            return ResponseResult.ok(orderResponses);
        }
        catch (Exception e) {
            log.error("Unexpected error getting user orders - userId: {}", userId, e);
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Get order by ID", description = "Get specific order details")
    @GetMapping("/{orderId}")
    public ResponseResult<OrderResponse> getOrderById(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            // Get entity from service
            Order order = orderService.findOrderByIdAndUserId(orderId, userId);

            // Convert to response DTO
            OrderResponse response = convertToOrderResponse(order);
            return ResponseResult.ok(response);
        }
        catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        }
        catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL);
        }
    }

    @Operation(summary = "Get order by order number", description = "Get order details by order number")
    @GetMapping("/orderNo/{orderNo}")
    public ResponseResult<OrderResponse> getOrderByOrderNo(
            @Parameter(description = "Order number") @PathVariable String orderNo) {
        try {
            // Get entity from service
            Order order = orderService.findOrderByOrderNo(orderNo);

            // Convert to response DTO
            OrderResponse response = convertToOrderResponse(order);
            return ResponseResult.ok(response);
        }
        catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        }
        catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL);
        }
    }

    @Operation(summary = "Get order items", description = "Get all items in an order")
    @GetMapping("/{orderId}/items")
    public ResponseResult<List<OrderItemResponse>> getOrderItems(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            // Get entities from service
            List<OrderItem> items = orderService.findOrderItems(orderId);

            // Convert to response DTOs
            List<OrderItemResponse> responses = convertToOrderItemResponses(items);
            return ResponseResult.ok(responses);
        }
        catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL);
        }
    }

    @Operation(summary = "Pay order", description = "Process payment for an order")
    @PutMapping("/{orderId}/pay")
    public ResponseResult<String> payOrder(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            // Service returns entity
            Order paidOrder = orderService.payOrder(userId, orderId);
            return ResponseResult.ok("Order payment processed successfully. Order No: " + paidOrder.getOrderNo());
        }
        catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        }
        catch (OrderStatusException e) {
            return ResponseResult.error(ResultCode.ORDER_STATUS_ERROR.getCode(), e.getMessage());
        }
        catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Cancel order", description = "Cancel an order")
    @PutMapping("/{orderId}/cancel")
    public ResponseResult<String> cancelOrder(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            // Use facade for complex business logic (restore stock)
            orderFacade.cancelOrder(userId, orderId);
            return ResponseResult.ok("Order cancelled successfully");
        }
        catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        }
        catch (OrderStatusException e) {
            return ResponseResult.error(ResultCode.ORDER_CANNOT_CANCEL.getCode(), e.getMessage());
        }
        catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Ship order", description = "Mark order as shipped (Admin only)")
    @PreAuthorize("hasRole('ORDER_ADMIN') or hasRole('SUPER_ADMIN')")
    @PutMapping("/{orderId}/ship")
    public ResponseResult<String> shipOrder(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            // Service returns entity
            Order shippedOrder = orderService.shipOrder(orderId);
            return ResponseResult.ok("Order shipped successfully. Order No: " + shippedOrder.getOrderNo());
        }
        catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        }
        catch (OrderStatusException e) {
            return ResponseResult.error(ResultCode.ORDER_STATUS_ERROR.getCode(), e.getMessage());
        }
        catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Complete order", description = "Mark order as completed")
    @PutMapping("/{orderId}/complete")
    public ResponseResult<String> completeOrder(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            // Service returns entity
            Order completedOrder = orderService.completeOrder(userId, orderId);
            return ResponseResult.ok("Order completed successfully. Order No: " + completedOrder.getOrderNo());
        }
        catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        }
        catch (OrderStatusException e) {
            return ResponseResult.error(ResultCode.ORDER_STATUS_ERROR.getCode(), e.getMessage());
        }
        catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Get order count", description = "Get total order count for user")
    @GetMapping("/count")
    public ResponseResult<Long> getOrderCount(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order status filter") @RequestParam(required = false) Integer status) {
        try {
            long count;
            if (status != null) {
                count = orderService.countOrdersByUserIdAndStatus(userId, status);
            }
            else {
                count = orderService.countOrdersByUserId(userId);
            }
            return ResponseResult.ok(count);
        }
        catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL);
        }
    }

    @Operation(summary = "Generate token for creating order")
    @PostMapping("/generateOrderToken")
    public ResponseResult<String> getOrderToken(@Parameter(description = "User ID") @RequestParam Long userId) {
        try {
            String orderToken = orderService.generateOrderToken(userId);
            return ResponseResult.ok(orderToken);
        }
        catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL);
        }
    }

    // ============= Helper methods for DTO conversion (in Controller layer) =============

    /**
     * Convert Order entity to OrderResponse DTO
     * This conversion logic is in the Controller layer following Pattern 1
     */
    private OrderResponse convertToOrderResponse(Order order) {
        OrderResponse response = new OrderResponse();
        BeanUtils.copyProperties(order, response);

        // Set computed fields
        response.setStatusText(order.getStatusText());
        response.setCanBeCancelled(order.canBeCancelled());
        response.setCanBeShipped(order.canBeShipped());
        response.setCanBeCompleted(order.canBeCompleted());

        // Load order items
        List<OrderItem> items = orderService.findOrderItems(order.getId());
        List<OrderItemResponse> itemResponses = convertToOrderItemResponses(items);
        response.setItems(itemResponses);
        response.setTotalItems(itemResponses.size());

        return response;
    }

    /**
     * Convert OrderItem entity to OrderItemResponse DTO
     */
    private OrderItemResponse convertToOrderItemResponse(OrderItem item) {
        OrderItemResponse response = new OrderItemResponse();
        BeanUtils.copyProperties(item, response);
        return response;
    }

    /**
     * Convert list of OrderItem entities to OrderItemResponse DTOs
     */
    private List<OrderItemResponse> convertToOrderItemResponses(List<OrderItem> items) {
        List<OrderItemResponse> responses = new ArrayList<>();
        for (OrderItem item : items) {
            responses.add(convertToOrderItemResponse(item));
        }
        return responses;
    }
}
