package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.OrderRequest;
import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.OrderItem;
import com.abel.ecommerce.exception.OrderNotFoundException;
import com.abel.ecommerce.exception.OrderStatusException;
import com.abel.ecommerce.facade.OrderFacade;
import com.abel.ecommerce.filter.RateLimitFilter;
import com.abel.ecommerce.service.OrderService;
import com.abel.ecommerce.service.TokenBlacklistService;
import com.abel.ecommerce.service.UserRoleCacheService;
import com.abel.ecommerce.utils.ResultCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnableMethodSecurity
@WebMvcTest(controllers = OrderController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitFilter.class))
@DisplayName("OrderController Web Layer Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderFacade orderFacade;

    @MockitoBean
    private UserRoleCacheService userRoleCacheService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    private Order testOrder;
    private OrderRequest testOrderRequest;
    private OrderItem testOrderItem1;
    private OrderItem testOrderItem2;
    private List<OrderItem> testOrderItems;
    private Long testUserId;
    private String testOrderNo;
    private String testIdempotencyToken;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testOrderNo = "20251229143022012345";
        testIdempotencyToken = "ORDER_TOKEN_1735468800000_1_abcd1234";

        testOrderItem1 = new OrderItem();
        testOrderItem1.setId(1L);
        testOrderItem1.setOrderId(1L);
        testOrderItem1.setOrderNo(testOrderNo);
        testOrderItem1.setProductId(100L);
        testOrderItem1.setProductName("Test Product 1");
        testOrderItem1.setProductImage("product1.jpg");
        testOrderItem1.setProductPrice(new BigDecimal("99.99"));
        testOrderItem1.setQuantity(2);

        testOrderItem2 = new OrderItem();
        testOrderItem2.setId(2L);
        testOrderItem2.setOrderId(1L);
        testOrderItem2.setOrderNo(testOrderNo);
        testOrderItem2.setProductId(101L);
        testOrderItem2.setProductName("Test Product 2");
        testOrderItem2.setProductImage("product2.jpg");
        testOrderItem2.setProductPrice(new BigDecimal("49.99"));
        testOrderItem2.setQuantity(1);

        testOrderItems = Arrays.asList(testOrderItem1, testOrderItem2);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setOrderNo(testOrderNo);
        testOrder.setUserId(testUserId);
        testOrder.setTotalAmount(new BigDecimal("259.97"));
        testOrder.setPayAmount(new BigDecimal("259.97"));
        testOrder.setFreightAmount(new BigDecimal("10.00"));
        testOrder.setStatus(Order.STATUS_PENDING_PAYMENT);
        testOrder.setReceiverName("John Doe");
        testOrder.setReceiverPhone("1234567890");
        testOrder.setReceiverAddress("123 Test Street, Test City");
        testOrder.setComment("Test order comment");
        testOrder.setCreatedAt(LocalDateTime.now().minusDays(1));
        testOrder.setUpdatedAt(LocalDateTime.now());

        testOrderRequest = new OrderRequest();
        testOrderRequest.setAddressId(1L);
        testOrderRequest.setComment("Test order");
    }

    // ========== GENERATE ORDER TOKEN TESTS ==========

    @Test
    @DisplayName("Should generate order token successfully")
    @WithMockUser
    void generateOrderToken_Success() throws Exception {
        when(orderService.generateOrderToken(testUserId)).thenReturn(testIdempotencyToken);

        mockMvc.perform(post("/api/orders/generateOrderToken")
                        .param("userId", String.valueOf(testUserId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(testIdempotencyToken));

        verify(orderService, times(1)).generateOrderToken(testUserId);
    }

    // ========== CREATE ORDER TESTS ==========

    @Test
    @DisplayName("Should create order successfully with valid idempotency token")
    @WithMockUser
    void createOrder_Success() throws Exception {
        when(orderService.validateAndDeleteOrderToken(testIdempotencyToken)).thenReturn(true);
        when(orderFacade.createOrder(testUserId, testOrderRequest)).thenReturn(testOrder);
        when(orderService.findOrderItems(1L)).thenReturn(testOrderItems);

        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Token", testIdempotencyToken)
                        .param("userId", String.valueOf(testUserId))
                        .content(objectMapper.writeValueAsString(testOrderRequest))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderNo").value(testOrderNo))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.totalItems").value(2));

        verify(orderService, times(1)).validateAndDeleteOrderToken(testIdempotencyToken);
        verify(orderFacade, times(1)).createOrder(testUserId, testOrderRequest);
        verify(orderService, times(1)).findOrderItems(1L);
    }

    @Test
    @DisplayName("Should return error when idempotency token is missing")
    @WithMockUser
    void createOrder_MissingIdempotencyToken_ReturnsError() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .param("userId", String.valueOf(testUserId))
                        .content(objectMapper.writeValueAsString(testOrderRequest))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value(containsString("Missing idempotency token")));

        verify(orderFacade, never()).createOrder(anyLong(), any(OrderRequest.class));
    }

    @Test
    @DisplayName("Should return error when idempotency token is invalid or already used")
    @WithMockUser
    void createOrder_InvalidIdempotencyToken_ReturnsError() throws Exception {
        when(orderService.validateAndDeleteOrderToken(testIdempotencyToken)).thenReturn(false);

        mockMvc.perform(post("/api/orders")
                        .header("Idempotency-Token", testIdempotencyToken)
                        .param("userId", String.valueOf(testUserId))
                        .content(objectMapper.writeValueAsString(testOrderRequest))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value(containsString("invalid")));

        verify(orderFacade, never()).createOrder(anyLong(), any(OrderRequest.class));
    }

    // ========== GET USER ORDERS TESTS ==========

    @Test
    @DisplayName("Should get user orders with pagination successfully")
    @WithMockUser
    void getUserOrders_Success() throws Exception {
        Order order2 = new Order();
        order2.setId(2L);
        order2.setOrderNo("20251229143022022345");
        order2.setUserId(testUserId);
        order2.setTotalAmount(new BigDecimal("100.00"));
        order2.setPayAmount(new BigDecimal("100.00"));
        order2.setFreightAmount(new BigDecimal("10.00"));
        order2.setStatus(Order.STATUS_PAID);
        order2.setReceiverName("Jane Doe");
        order2.setReceiverPhone("0987654321");
        order2.setReceiverAddress("456 Test Ave");

        List<Order> orders = Arrays.asList(testOrder, order2);
        Page<Order> orderPage = new PageImpl<>(orders, PageRequest.of(0, 10), 2);

        when(orderService.findOrdersByUserId(eq(testUserId), any(PageRequest.class)))
                .thenReturn(orderPage);
        when(orderService.findOrderItems(anyLong())).thenReturn(testOrderItems);

        mockMvc.perform(get("/api/orders")
                        .param("userId", String.valueOf(testUserId))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[0].orderNo").value(testOrderNo));

        verify(orderService, times(1)).findOrdersByUserId(eq(testUserId), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should get user orders filtered by status")
    @WithMockUser
    void getUserOrders_WithStatusFilter_Success() throws Exception {
        Page<Order> orderPage = new PageImpl<>(Arrays.asList(testOrder));
        when(orderService.findOrdersByUserIdAndStatus(
                eq(testUserId),
                eq(Order.STATUS_PENDING_PAYMENT),
                any(PageRequest.class)
        )).thenReturn(orderPage);
        when(orderService.findOrderItems(anyLong())).thenReturn(testOrderItems);

        mockMvc.perform(get("/api/orders")
                        .param("userId", String.valueOf(testUserId))
                        .param("status", String.valueOf(Order.STATUS_PENDING_PAYMENT))
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].status").value(Order.STATUS_PENDING_PAYMENT));

        verify(orderService, times(1)).findOrdersByUserIdAndStatus(
                eq(testUserId),
                eq(Order.STATUS_PENDING_PAYMENT),
                any(PageRequest.class)
        );
    }

    @Test
    @DisplayName("Should return empty page when user has no orders")
    @WithMockUser
    void getUserOrders_EmptyResult() throws Exception {
        Page<Order> emptyPage = new PageImpl<>(Arrays.asList());
        when(orderService.findOrdersByUserId(eq(testUserId), any(PageRequest.class)))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/api/orders")
                        .param("userId", String.valueOf(testUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content", hasSize(0)))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ========== GET ORDER BY ID TESTS ==========

    @Test
    @DisplayName("Should get order by ID successfully")
    @WithMockUser
    void getOrderById_Success() throws Exception {
        when(orderService.findOrderByIdAndUserId(1L, testUserId)).thenReturn(testOrder);
        when(orderService.findOrderItems(1L)).thenReturn(testOrderItems);

        mockMvc.perform(get("/api/orders/{orderId}", 1L)
                        .param("userId", String.valueOf(testUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderNo").value(testOrderNo))
                .andExpect(jsonPath("$.data.userId").value(testUserId))
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.totalItems").value(2));

        verify(orderService, times(1)).findOrderByIdAndUserId(1L, testUserId);
        verify(orderService, times(1)).findOrderItems(1L);
    }

    @Test
    @DisplayName("Should return 404 when order not found")
    @WithMockUser
    void getOrderById_NotFound() throws Exception {
        when(orderService.findOrderByIdAndUserId(999L, testUserId))
                .thenThrow(new OrderNotFoundException("Order not found or doesn't belong to user"));

        mockMvc.perform(get("/api/orders/{orderId}", 999L)
                        .param("userId", String.valueOf(testUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ResultCode.ORDER_NOT_EXIST.getCode()));

        verify(orderService, times(1)).findOrderByIdAndUserId(999L, testUserId);
    }

    @Test
    @DisplayName("Should return 404 when user tries to access another user's order")
    @WithMockUser
    void getOrderById_UnauthorizedAccess() throws Exception {
        Long unauthorizedUserId = 2L;
        when(orderService.findOrderByIdAndUserId(1L, unauthorizedUserId))
                .thenThrow(new OrderNotFoundException("Order not found or doesn't belong to user"));

        mockMvc.perform(get("/api/orders/{orderId}", 1L)
                        .param("userId", String.valueOf(unauthorizedUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).findOrderByIdAndUserId(1L, unauthorizedUserId);
    }

    // ========== GET ORDER BY ORDER NUMBER TESTS ==========

    @Test
    @DisplayName("Should get order by order number successfully")
    @WithMockUser
    void getOrderByOrderNo_Success() throws Exception {
        when(orderService.findOrderByOrderNo(testOrderNo)).thenReturn(testOrder);
        when(orderService.findOrderItems(1L)).thenReturn(testOrderItems);

        mockMvc.perform(get("/api/orders/orderNo/{orderNo}", testOrderNo)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.orderNo").value(testOrderNo))
                .andExpect(jsonPath("$.data.items", hasSize(2)));

        verify(orderService, times(1)).findOrderByOrderNo(testOrderNo);
        verify(orderService, times(1)).findOrderItems(1L);
    }

    @Test
    @DisplayName("Should return 404 when order number not found")
    @WithMockUser
    void getOrderByOrderNo_NotFound() throws Exception {
        String invalidOrderNo = "INVALID_ORDER_NO";
        when(orderService.findOrderByOrderNo(invalidOrderNo))
                .thenThrow(new OrderNotFoundException(invalidOrderNo, "order number"));

        mockMvc.perform(get("/api/orders/orderNo/{orderNo}", invalidOrderNo)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ResultCode.ORDER_NOT_EXIST.getCode()));

        verify(orderService, times(1)).findOrderByOrderNo(invalidOrderNo);
    }

    // ========== GET ORDER ITEMS TESTS ==========

    @Test
    @DisplayName("Should get order items successfully")
    @WithMockUser
    void getOrderItems_Success() throws Exception {
        when(orderService.findOrderItems(1L)).thenReturn(testOrderItems);

        mockMvc.perform(get("/api/orders/{orderId}/items", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].productName").value("Test Product 1"))
                .andExpect(jsonPath("$.data[1].productName").value("Test Product 2"));

        verify(orderService, times(1)).findOrderItems(1L);
    }

    @Test
    @DisplayName("Should return empty list when order has no items")
    @WithMockUser
    void getOrderItems_EmptyList() throws Exception {
        when(orderService.findOrderItems(999L)).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/orders/{orderId}/items", 999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(orderService, times(1)).findOrderItems(999L);
    }

    // ========== PAY ORDER TESTS ==========

    @Test
    @DisplayName("Should pay order successfully")
    @WithMockUser
    void payOrder_Success() throws Exception {
        Order paidOrder = new Order();
        paidOrder.setId(1L);
        paidOrder.setOrderNo(testOrderNo);
        paidOrder.setStatus(Order.STATUS_PAID);
        paidOrder.setPaymentTime(LocalDateTime.now());

        when(orderService.payOrder(testUserId, 1L)).thenReturn(paidOrder);

        mockMvc.perform(put("/api/orders/{orderId}/pay", 1L)
                        .param("userId", String.valueOf(testUserId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(containsString("payment processed successfully")))
                .andExpect(jsonPath("$.data").value(containsString(testOrderNo)));

        verify(orderService, times(1)).payOrder(testUserId, 1L);
    }

    @Test
    @DisplayName("Should return error when paying already paid order")
    @WithMockUser
    void payOrder_AlreadyPaid_ReturnsError() throws Exception {
        when(orderService.payOrder(testUserId, 1L))
                .thenThrow(new OrderStatusException("Payment failed: Order is not in pending payment status"));

        mockMvc.perform(put("/api/orders/{orderId}/pay", 1L)
                        .param("userId", String.valueOf(testUserId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ResultCode.ORDER_STATUS_ERROR.getCode()))
                .andExpect(jsonPath("$.msg").value(containsString("not in pending payment status")));

        verify(orderService, times(1)).payOrder(testUserId, 1L);
    }

    // ========== CANCEL ORDER TESTS ==========

    @Test
    @DisplayName("Should cancel order successfully via OrderFacade")
    @WithMockUser
    void cancelOrder_Success() throws Exception {
        doNothing().when(orderFacade).cancelOrder(testUserId, 1L);

        mockMvc.perform(put("/api/orders/{orderId}/cancel", 1L)
                        .param("userId", String.valueOf(testUserId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(containsString("cancelled successfully")));

        verify(orderFacade, times(1)).cancelOrder(testUserId, 1L);
    }

    @Test
    @DisplayName("Should return error when cancelling shipped order")
    @WithMockUser
    void cancelOrder_AlreadyShipped_ReturnsError() throws Exception {
        doThrow(new OrderStatusException("Cannot cancel order that has been shipped"))
                .when(orderFacade).cancelOrder(testUserId, 1L);

        mockMvc.perform(put("/api/orders/{orderId}/cancel", 1L)
                        .param("userId", String.valueOf(testUserId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ResultCode.ORDER_STATUS_ERROR.getCode()))
                .andExpect(jsonPath("$.msg").value(containsString("Cannot cancel")));

        verify(orderFacade, times(1)).cancelOrder(testUserId, 1L);
    }

    // ========== SHIP ORDER TESTS (ADMIN ONLY) ==========

    @Test
    @DisplayName("Should ship order successfully with ORDER_ADMIN role")
    @WithMockUser(roles = {"ORDER_ADMIN"})
    void shipOrder_Success_WithAdminRole() throws Exception {
        Order shippedOrder = new Order();
        shippedOrder.setId(1L);
        shippedOrder.setOrderNo(testOrderNo);
        shippedOrder.setStatus(Order.STATUS_SHIPPED);
        shippedOrder.setDeliveryTime(LocalDateTime.now());

        when(orderService.shipOrder(1L)).thenReturn(shippedOrder);

        mockMvc.perform(put("/api/orders/{orderId}/ship", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(containsString("shipped successfully")))
                .andExpect(jsonPath("$.data").value(containsString(testOrderNo)));

        verify(orderService, times(1)).shipOrder(1L);
    }

    @Test
    @DisplayName("Should ship order successfully with SUPER_ADMIN role")
    @WithMockUser(roles = {"SUPER_ADMIN"})
    void shipOrder_Success_WithSuperAdminRole() throws Exception {
        Order shippedOrder = new Order();
        shippedOrder.setId(1L);
        shippedOrder.setOrderNo(testOrderNo);
        shippedOrder.setStatus(Order.STATUS_SHIPPED);

        when(orderService.shipOrder(1L)).thenReturn(shippedOrder);

        mockMvc.perform(put("/api/orders/{orderId}/ship", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        verify(orderService, times(1)).shipOrder(1L);
    }

    @Test
    @DisplayName("Should return 403 when user without admin role tries to ship order")
    @WithMockUser(roles = {"USER"})
    void shipOrder_Forbidden_WithoutAdminRole() throws Exception {
        mockMvc.perform(put("/api/orders/{orderId}/ship", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(orderService, never()).shipOrder(anyLong());
    }

    @Test
    @DisplayName("Should return 404 when shipping non-existent order")
    @WithMockUser(roles = {"ORDER_ADMIN"})
    void shipOrder_NotFound() throws Exception {
        when(orderService.shipOrder(999L))
                .thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(put("/api/orders/{orderId}/ship", 999L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(ResultCode.ORDER_NOT_EXIST.getCode()));

        verify(orderService, times(1)).shipOrder(999L);
    }

    @Test
    @DisplayName("Should return 400 when shipping order in invalid status")
    @WithMockUser(roles = {"ORDER_ADMIN"})
    void shipOrder_InvalidStatus() throws Exception {
        when(orderService.shipOrder(1L))
                .thenThrow(new OrderStatusException("Only paid order can be shipped"));

        mockMvc.perform(put("/api/orders/{orderId}/ship", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ResultCode.ORDER_STATUS_ERROR.getCode()));

        verify(orderService, times(1)).shipOrder(1L);
    }

    // ========== COMPLETE ORDER TESTS ==========

    @Test
    @DisplayName("Should complete order successfully")
    @WithMockUser
    void completeOrder_Success() throws Exception {
        Order completedOrder = new Order();
        completedOrder.setId(1L);
        completedOrder.setOrderNo(testOrderNo);
        completedOrder.setStatus(Order.STATUS_COMPLETED);
        completedOrder.setReceiveTime(LocalDateTime.now());

        when(orderService.completeOrder(testUserId, 1L)).thenReturn(completedOrder);

        mockMvc.perform(put("/api/orders/{orderId}/complete", 1L)
                        .param("userId", String.valueOf(testUserId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(containsString("completed successfully")))
                .andExpect(jsonPath("$.data").value(containsString(testOrderNo)));

        verify(orderService, times(1)).completeOrder(testUserId, 1L);
    }

    @Test
    @DisplayName("Should return error when completing order in invalid status")
    @WithMockUser
    void completeOrder_InvalidStatus() throws Exception {
        when(orderService.completeOrder(testUserId, 1L))
                .thenThrow(OrderStatusException.cannotComplete(testOrderNo));

        mockMvc.perform(put("/api/orders/{orderId}/complete", 1L)
                        .param("userId", String.valueOf(testUserId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(ResultCode.ORDER_STATUS_ERROR.getCode()));

        verify(orderService, times(1)).completeOrder(testUserId, 1L);
    }

    @Test
    @DisplayName("Should return 404 when completing non-existent order")
    @WithMockUser
    void completeOrder_NotFound() throws Exception {
        when(orderService.completeOrder(testUserId, 999L))
                .thenThrow(new OrderNotFoundException("Order not found or doesn't belong to user"));

        mockMvc.perform(put("/api/orders/{orderId}/complete", 999L)
                        .param("userId", String.valueOf(testUserId))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(orderService, times(1)).completeOrder(testUserId, 999L);
    }

    // ========== GET ORDER COUNT TESTS ==========

    @Test
    @DisplayName("Should get order count for user")
    @WithMockUser
    void getOrderCount_Success() throws Exception {
        when(orderService.countOrdersByUserId(testUserId)).thenReturn(5L);

        mockMvc.perform(get("/api/orders/count")
                        .param("userId", String.valueOf(testUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(5));

        verify(orderService, times(1)).countOrdersByUserId(testUserId);
    }

    @Test
    @DisplayName("Should get order count filtered by status")
    @WithMockUser
    void getOrderCount_WithStatusFilter() throws Exception {
        when(orderService.countOrdersByUserIdAndStatus(testUserId, Order.STATUS_PAID))
                .thenReturn(3L);

        mockMvc.perform(get("/api/orders/count")
                        .param("userId", String.valueOf(testUserId))
                        .param("status", String.valueOf(Order.STATUS_PAID))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(3));

        verify(orderService, times(1)).countOrdersByUserIdAndStatus(testUserId, Order.STATUS_PAID);
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    @WithMockUser
    void serviceException_ReturnsInternalServerError() throws Exception {
        when(orderService.findOrderByIdAndUserId(1L, testUserId))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/api/orders/{orderId}", 1L)
                        .param("userId", String.valueOf(testUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(999))
                .andExpect(jsonPath("$.msg").value("Database connection failed"));
    }

    @Test
    @DisplayName("Should handle invalid path variables")
    @WithMockUser
    void invalidPathVariable_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/orders/{orderId}", "invalid")
                        .param("userId", String.valueOf(testUserId))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
