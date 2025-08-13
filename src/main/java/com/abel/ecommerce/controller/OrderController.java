package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.OrderRequest;
import com.abel.ecommerce.dto.response.OrderItemResponse;
import com.abel.ecommerce.dto.response.OrderResponse;
import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.exception.OrderNotFoundException;
import com.abel.ecommerce.exception.OrderStatusException;
import com.abel.ecommerce.service.OrderService;
import com.abel.ecommerce.utils.ResponseResult;
import com.abel.ecommerce.utils.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "Order creation, payment, shipping and tracking")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create order from cart", description = "Create a new order from user's selected cart items")
    @PostMapping
    public ResponseResult<OrderResponse> createOrder(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order data") @Valid @RequestBody OrderRequest request) {
        try {
            Order order = orderService.createOrder(userId, request);
            OrderResponse response = new OrderResponse();
            BeanUtils.copyProperties(order, response);
            return ResponseResult.ok(response);
        } catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        } catch (Exception e) {
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
            Page<OrderResponse> orders;
            
            if (status != null) {
                orders = orderService.findOrdersByUserIdAndStatus(userId, status, pageable);
            } else {
                orders = orderService.findOrdersByUserId(userId, pageable);
            }
            
            return ResponseResult.ok(orders);
        } catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL);
        }
    }

    @Operation(summary = "Get order by ID", description = "Get specific order details")
    @GetMapping("/{orderId}")
    public ResponseResult<OrderResponse> getOrderById(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            OrderResponse order = orderService.findOrderById(userId, orderId);
            return ResponseResult.ok(order);
        } catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        } catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL);
        }
    }

    @Operation(summary = "Get order by order number", description = "Get order details by order number")
    @GetMapping("/orderNo/{orderNo}")
    public ResponseResult<OrderResponse> getOrderByOrderNo(
            @Parameter(description = "Order number") @PathVariable String orderNo) {
        try {
            OrderResponse order = orderService.findOrderByOrderNo(orderNo);
            return ResponseResult.ok(order);
        } catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        } catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL);
        }
    }

    @Operation(summary = "Get order items", description = "Get all items in an order")
    @GetMapping("/{orderId}/items")
    public ResponseResult<List<OrderItemResponse>> getOrderItems(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            List<OrderItemResponse> items = orderService.findOrderItems(orderId);
            return ResponseResult.ok(items);
        } catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL);
        }
    }

    @Operation(summary = "Pay order", description = "Process payment for an order")
    @PutMapping("/{orderId}/pay")
    public ResponseResult<String> payOrder(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            orderService.payOrder(userId, orderId);
            return ResponseResult.ok("Order payment processed successfully");
        } catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        } catch (OrderStatusException e) {
            return ResponseResult.error(ResultCode.ORDER_STATUS_ERROR.getCode(), e.getMessage());
        } catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Cancel order", description = "Cancel an order")
    @PutMapping("/{orderId}/cancel")
    public ResponseResult<String> cancelOrder(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            orderService.cancelOrder(userId, orderId);
            return ResponseResult.ok("Order cancelled successfully");
        } catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        } catch (OrderStatusException e) {
            return ResponseResult.error(ResultCode.ORDER_CANNOT_CANCEL.getCode(), e.getMessage());
        } catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Ship order", description = "Mark order as shipped")
    @PutMapping("/{orderId}/ship")
    public ResponseResult<String> shipOrder(
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            orderService.shipOrder(orderId);
            return ResponseResult.ok("Order shipped successfully");
        } catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        } catch (OrderStatusException e) {
            return ResponseResult.error(ResultCode.ORDER_STATUS_ERROR.getCode(), e.getMessage());
        } catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL.getCode(), e.getMessage());
        }
    }

    @Operation(summary = "Complete order", description = "Mark order as completed")
    @PutMapping("/{orderId}/complete")
    public ResponseResult<String> completeOrder(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order ID") @PathVariable Long orderId) {
        try {
            orderService.completeOrder(userId, orderId);
            return ResponseResult.ok("Order completed successfully");
        } catch (OrderNotFoundException e) {
            return ResponseResult.error(ResultCode.ORDER_NOT_EXIST.getCode(), e.getMessage());
        } catch (OrderStatusException e) {
            return ResponseResult.error(ResultCode.ORDER_STATUS_ERROR.getCode(), e.getMessage());
        } catch (Exception e) {
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
            } else {
                count = orderService.countOrdersByUserId(userId);
            }
            return ResponseResult.ok(count);
        } catch (Exception e) {
            return ResponseResult.error(ResultCode.COMMON_FAIL);
        }
    }
}
