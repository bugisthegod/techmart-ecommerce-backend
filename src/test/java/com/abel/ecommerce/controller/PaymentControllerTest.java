package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.CheckoutRequest;
import com.abel.ecommerce.dto.request.RefundRequest;
import com.abel.ecommerce.dto.response.CheckoutResponse;
import com.abel.ecommerce.dto.response.RefundResponse;
import com.abel.ecommerce.entity.Payment;
import com.abel.ecommerce.exception.OrderNotFoundException;
import com.abel.ecommerce.exception.OrderStatusException;
import com.abel.ecommerce.exception.PaymentNotFoundException;
import com.abel.ecommerce.exception.UnauthorizedAccessException;
import com.abel.ecommerce.filter.RateLimitFilter;
import com.abel.ecommerce.service.PaymentService;
import com.abel.ecommerce.service.RefundService;
import com.abel.ecommerce.service.TokenBlacklistService;
import com.abel.ecommerce.service.UserRoleCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnableMethodSecurity
@WebMvcTest(controllers = PaymentController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RateLimitFilter.class))
@DisplayName("PaymentController Web Layer Tests")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private RefundService refundService;

    @MockitoBean
    private UserRoleCacheService userRoleCacheService;

    @MockitoBean
    private TokenBlacklistService tokenBlacklistService;

    private Payment testPayment;
    private CheckoutRequest checkoutRequest;
    private CheckoutResponse checkoutResponse;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;

        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrderId(100L);
        testPayment.setStripeSessionId("cs_test_123456");
        testPayment.setStripePaymentIntentId("pi_test_123456");
        testPayment.setAmount(new BigDecimal("99.99"));
        testPayment.setCurrency("USD");
        testPayment.setStatus(Payment.STATUS_PENDING);
        testPayment.setCreatedAt(LocalDateTime.now());
        testPayment.setUpdatedAt(LocalDateTime.now());

        checkoutRequest = new CheckoutRequest();
        checkoutRequest.setOrderId(100L);

        checkoutResponse = new CheckoutResponse(
                "cs_test_123456",
                "https://checkout.stripe.com/pay/cs_test_123456"
        );
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Should create checkout session successfully")
    void testCreateCheckoutSession_Success() throws Exception {
        // Given
        when(paymentService.createCheckoutSession(100L, testUserId))
                .thenReturn(checkoutResponse);

        // When & Then
        mockMvc.perform(post("/api/payments/checkout")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data.sessionId").value("cs_test_123456"))
                .andExpect(jsonPath("$.data.checkoutUrl").value("https://checkout.stripe.com/pay/cs_test_123456"));

        verify(paymentService, times(1)).createCheckoutSession(100L, testUserId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Should return 400 when checkout request is invalid")
    void testCreateCheckoutSession_InvalidRequest() throws Exception {
        // Given - Request with null order ID
        CheckoutRequest invalidRequest = new CheckoutRequest();
        invalidRequest.setOrderId(null);

        // When & Then
        mockMvc.perform(post("/api/payments/checkout")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(paymentService, never()).createCheckoutSession(any(), any());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Should return 404 when order not found during checkout")
    void testCreateCheckoutSession_OrderNotFound() throws Exception {
        // Given
        when(paymentService.createCheckoutSession(999L, testUserId))
                .thenThrow(new OrderNotFoundException(999L));

        checkoutRequest.setOrderId(999L);

        // When & Then
        mockMvc.perform(post("/api/payments/checkout")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(7001))
                .andExpect(jsonPath("$.msg").value("Order not found with ID: 999"));

        verify(paymentService, times(1)).createCheckoutSession(999L, testUserId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Should return 500 when user unauthorized to access order")
    void testCreateCheckoutSession_UnauthorizedAccess() throws Exception {
        // Given
        when(paymentService.createCheckoutSession(100L, testUserId))
                .thenThrow(new UnauthorizedAccessException("You are not authorized to access this order"));

        // When & Then
        mockMvc.perform(post("/api/payments/checkout")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(999))
                .andExpect(jsonPath("$.msg").value("You are not authorized to access this order"));

        verify(paymentService, times(1)).createCheckoutSession(100L, testUserId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Should return 400 when order status is invalid for payment")
    void testCreateCheckoutSession_InvalidOrderStatus() throws Exception {
        // Given
        when(paymentService.createCheckoutSession(100L, testUserId))
                .thenThrow(new OrderStatusException("Order is not in pending payment status"));

        // When & Then
        mockMvc.perform(post("/api/payments/checkout")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(7002))
                .andExpect(jsonPath("$.msg").value("Order is not in pending payment status"));

        verify(paymentService, times(1)).createCheckoutSession(100L, testUserId);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Should get payment by order ID successfully")
    void testGetPaymentByOrderId_Success() throws Exception {
        // Given
        when(paymentService.getPaymentByOrderId(100L))
                .thenReturn(testPayment);

        // When & Then
        mockMvc.perform(get("/api/payments/order/{orderId}", 100L)
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.msg").value("Success"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.orderId").value(100))
                .andExpect(jsonPath("$.data.stripeSessionId").value("cs_test_123456"))
                .andExpect(jsonPath("$.data.amount").value(99.99))
                .andExpect(jsonPath("$.data.currency").value("USD"))
                .andExpect(jsonPath("$.data.status").value(Payment.STATUS_PENDING));

        verify(paymentService, times(1)).getPaymentByOrderId(100L);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Should return 500 when payment not found by order ID")
    void testGetPaymentByOrderId_NotFound() throws Exception {
        // Given
        when(paymentService.getPaymentByOrderId(999L))
                .thenThrow(new PaymentNotFoundException("Payment not found for order 999"));

        // When & Then
        mockMvc.perform(get("/api/payments/order/{orderId}", 999L)
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(999))
                .andExpect(jsonPath("$.msg").value("Payment not found for order 999"));

        verify(paymentService, times(1)).getPaymentByOrderId(999L);
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Should get payment by session ID successfully")
    void testGetPaymentBySessionId_Success() throws Exception {
        // Given
        when(paymentService.findByStripeSessionId("cs_test_123456"))
                .thenReturn(testPayment);

        // When & Then
        mockMvc.perform(get("/api/payments/session/{sessionId}", "cs_test_123456"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.stripeSessionId").value("cs_test_123456"))
                .andExpect(jsonPath("$.data.orderId").value(100))
                .andExpect(jsonPath("$.data.amount").value(99.99));

        verify(paymentService, times(1)).findByStripeSessionId("cs_test_123456");
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    @DisplayName("Should return 500 when session ID not found")
    void testGetPaymentBySessionId_NotFound() throws Exception {
        // Given
        when(paymentService.findByStripeSessionId("cs_test_invalid"))
                .thenThrow(new PaymentNotFoundException("Payment not found for session cs_test_invalid"));

        // When & Then
        mockMvc.perform(get("/api/payments/session/{sessionId}", "cs_test_invalid"))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(999))
                .andExpect(jsonPath("$.msg").value("Payment not found for session cs_test_invalid"));

        verify(paymentService, times(1)).findByStripeSessionId("cs_test_invalid");
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Should process refund successfully as admin")
    void testProcessRefund_Success() throws Exception {
        // Given
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setReason("Customer requested refund");

        RefundResponse refundResponse = new RefundResponse();
        refundResponse.setRefundId("re_test_123");
        refundResponse.setAmount(new BigDecimal("99.99"));
        refundResponse.setStatus("succeeded");

        when(refundService.processRefund(1L, "Customer requested refund"))
                .thenReturn(refundResponse);

        // When & Then
        mockMvc.perform(post("/api/payments/{paymentId}/refund", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.refundId").value("re_test_123"))
                .andExpect(jsonPath("$.data.amount").value(99.99))
                .andExpect(jsonPath("$.data.status").value("succeeded"));

        verify(refundService, times(1)).processRefund(1L, "Customer requested refund");
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    @DisplayName("Should return 403 when non-admin tries to process refund")
    void testProcessRefund_Forbidden() throws Exception {
        // Given
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setReason("Customer requested refund");

        // When & Then
        mockMvc.perform(post("/api/payments/{paymentId}/refund", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());

        verify(refundService, never()).processRefund(any(), any());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Should return 500 when refunding non-existent payment")
    void testProcessRefund_PaymentNotFound() throws Exception {
        // Given
        RefundRequest refundRequest = new RefundRequest();
        refundRequest.setReason("Customer requested refund");

        when(refundService.processRefund(999L, "Customer requested refund"))
                .thenThrow(new PaymentNotFoundException("Payment not found with id 999"));

        // When & Then
        mockMvc.perform(post("/api/payments/{paymentId}/refund", 999L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refundRequest)))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(999))
                .andExpect(jsonPath("$.msg").value("Payment not found with id 999"));

        verify(refundService, times(1)).processRefund(999L, "Customer requested refund");
    }

    @Test
    @DisplayName("Should require authentication for checkout endpoint")
    void testCreateCheckoutSession_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/payments/checkout")
                        .with(csrf())
                        .param("userId", testUserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(paymentService, never()).createCheckoutSession(any(), any());
    }

    @Test
    @DisplayName("Should require authentication for get payment by order ID")
    void testGetPaymentByOrderId_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/payments/order/{orderId}", 100L)
                        .param("userId", testUserId.toString()))
                .andDo(print())
                .andExpect(status().isUnauthorized());

        verify(paymentService, never()).getPaymentByOrderId(any());
    }
}