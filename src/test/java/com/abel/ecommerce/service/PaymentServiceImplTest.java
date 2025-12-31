package com.abel.ecommerce.service;

import com.abel.ecommerce.config.StripeConfig;
import com.abel.ecommerce.dto.response.CheckoutResponse;
import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.Payment;
import com.abel.ecommerce.entity.User;
import com.abel.ecommerce.exception.*;
import com.abel.ecommerce.repository.OrderRepository;
import com.abel.ecommerce.repository.PaymentRepository;
import com.abel.ecommerce.repository.UserRepository;
import com.abel.ecommerce.service.impl.PaymentServiceImpl;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PaymentServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StripeConfig stripeConfig;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment testPayment;
    private Order testOrder;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Setup test payment
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrderId(100L);
        testPayment.setStripeSessionId("cs_test_123");
        testPayment.setStripePaymentIntentId("pi_test_123");
        testPayment.setAmount(new BigDecimal("99.99"));
        testPayment.setCurrency("USD");
        testPayment.setStatus(Payment.STATUS_PENDING);

        // Setup test order
        testOrder = new Order();
        testOrder.setId(100L);
        testOrder.setUserId(1L);
        testOrder.setOrderNo("ORD20231215001");
        testOrder.setPayAmount(new BigDecimal("99.99"));
        testOrder.setStatus(Order.STATUS_PENDING_PAYMENT);

        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should get payment by order ID successfully")
    void testGetPaymentByOrderId_Success() {
        // Given
        when(paymentRepository.findByOrderId(100L)).thenReturn(Optional.of(testPayment));

        // When
        Payment result = paymentService.getPaymentByOrderId(100L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(100L);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        verify(paymentRepository, times(1)).findByOrderId(100L);
    }

    @Test
    @DisplayName("Should throw PaymentNotFoundException when payment not found by order ID")
    void testGetPaymentByOrderId_NotFound() {
        // Given
        when(paymentRepository.findByOrderId(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentByOrderId(999L))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found for order 999");

        verify(paymentRepository, times(1)).findByOrderId(999L);
    }

    @Test
    @DisplayName("Should find payment by Stripe session ID successfully")
    void testFindByStripeSessionId_Success() {
        // Given
        when(paymentRepository.findByStripeSessionId("cs_test_123"))
                .thenReturn(Optional.of(testPayment));

        // When
        Payment result = paymentService.findByStripeSessionId("cs_test_123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStripeSessionId()).isEqualTo("cs_test_123");
        verify(paymentRepository, times(1)).findByStripeSessionId("cs_test_123");
    }

    @Test
    @DisplayName("Should throw PaymentNotFoundException when session ID not found")
    void testFindByStripeSessionId_NotFound() {
        // Given
        when(paymentRepository.findByStripeSessionId("cs_test_invalid"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.findByStripeSessionId("cs_test_invalid"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found for session cs_test_invalid");

        verify(paymentRepository, times(1)).findByStripeSessionId("cs_test_invalid");
    }

    @Test
    @DisplayName("Should update payment status successfully")
    void testUpdatePaymentStatus_Success() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // When
        Payment result = paymentService.updatePaymentStatus(1L, Payment.STATUS_SUCCEEDED, "pi_new_123");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(Payment.STATUS_SUCCEEDED);
        assertThat(result.getStripePaymentIntentId()).isEqualTo("pi_new_123");

        verify(paymentRepository, times(1)).findById(1L);
        verify(paymentRepository, times(1)).save(testPayment);
    }

    @Test
    @DisplayName("Should update payment status without changing payment intent ID when null")
    void testUpdatePaymentStatus_NullPaymentIntentId() {
        // Given
        String originalIntentId = "pi_test_123";
        testPayment.setStripePaymentIntentId(originalIntentId);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(testPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // When
        Payment result = paymentService.updatePaymentStatus(1L, Payment.STATUS_FAILED, null);

        // Then
        assertThat(result.getStatus()).isEqualTo(Payment.STATUS_FAILED);
        assertThat(result.getStripePaymentIntentId()).isEqualTo(originalIntentId); // Should remain unchanged

        verify(paymentRepository, times(1)).save(testPayment);
    }

    @Test
    @DisplayName("Should throw PaymentNotFoundException when updating non-existent payment")
    void testUpdatePaymentStatus_PaymentNotFound() {
        // Given
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
                paymentService.updatePaymentStatus(999L, Payment.STATUS_SUCCEEDED, "pi_test_123"))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("Payment not found with id 999");

        verify(paymentRepository, times(1)).findById(999L);
        verify(paymentRepository, never()).save(any());
    }

    /*
     * Implement test for createCheckoutSession - Success case
     */
    @Test
    @DisplayName("Should create checkout session successfully")
    void testCreateCheckoutSession_Success() {
        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class);
             MockedStatic<Session> sessionMock = mockStatic(Session.class)) {
            // Given
            when(orderRepository.findById(testPayment.getOrderId())).thenReturn(Optional.of(testOrder));
            when(paymentRepository.existsByOrderIdAndStatus(testPayment.getOrderId(), Payment.STATUS_SUCCEEDED)).thenReturn(false);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

            // Mock Stripe Customer
            Customer mockCustomer = mock(Customer.class);
            when(mockCustomer.getId()).thenReturn("cus_new_123");
            customerMock.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenReturn(mockCustomer);

            // When & Then
            testUser.setStripeCustomerId(null);

            Session mockSession = mock(Session.class);
            when(mockSession.getId()).thenReturn("cs_test_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6");
            when(mockSession.getUrl()).thenReturn("https://checkout.stripe.com/pay/cs_test_");
            sessionMock.when(() -> Session.create(any(SessionCreateParams.class)))
                    .thenReturn(mockSession);

            // testPayment.setStripeSessionId("cs_test_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6");
            when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

            CheckoutResponse checkoutSession = paymentService.createCheckoutSession(testPayment.getOrderId(), testUser.getId());

            assertThat(checkoutSession.getSessionId()).isEqualTo("cs_test_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6");
            assertThat(checkoutSession.getCheckoutUrl()).isEqualTo("https://checkout.stripe.com/pay/cs_test_");

            verify(orderRepository, times(1)).findById(testPayment.getOrderId());
            verify(paymentRepository, times(1)).existsByOrderIdAndStatus(testPayment.getOrderId(), Payment.STATUS_SUCCEEDED);
            verify(userRepository, times(1)).save(any(User.class));
            verify(userRepository, times(1)).findById(testUser.getId());
            verify(paymentRepository, times(1)).save(any(Payment.class));
            customerMock.verify(() -> Customer.create(any(CustomerCreateParams.class)), times(1));
            sessionMock.verify(() -> Session.create(any(SessionCreateParams.class)), times(1));

        }

    }

    /*
     * Should throw OrderNotFoundException when order doesn't exist
     */
    @Test
    @DisplayName("Should throw OrderNotFoundException when order not found")
    void testCreateCheckoutSession_OrderNotFound() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.createCheckoutSession(999L, testUser.getId()))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found with ID: 999");

        verify(orderRepository, times(1)).findById(999L);

    }

    /*
     * Should throw UnauthorizedAccessException when order doesn't belong to user
     */
    @Test
    @DisplayName("Should throw UnauthorizedAccessException when order belongs to different user")
    void testCreateCheckoutSession_UnauthorizedAccess() {
        // Given
        when(orderRepository.findById(testPayment.getOrderId())).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> paymentService.createCheckoutSession(testPayment.getOrderId(), 999L))
                .isInstanceOf(UnauthorizedAccessException.class)
                .hasMessageContaining("You are not authorized to access this order");

        verify(orderRepository, times(1)).findById(testPayment.getOrderId());
    }

    /*
     * Should throw OrderStatusException when order status is not pending payment
     */
    @Test
    @DisplayName("Should throw OrderStatusException when order is not pending payment")
    void testCreateCheckoutSession_InvalidOrderStatus() {
        // Given
        testOrder.setStatus(Order.STATUS_COMPLETED); // Not pending status
        when(orderRepository.findById(testPayment.getOrderId())).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThatThrownBy(() -> paymentService.createCheckoutSession(testPayment.getOrderId(), testUser.getId()))
                .isInstanceOf(OrderStatusException.class)
                .hasMessageContaining("Order is not in pending payment status");

        verify(orderRepository, times(1)).findById(testPayment.getOrderId());
    }

    /*
     * Should throw IllegalStateException when payment already completed for order
     */
    @Test
    @DisplayName("Should throw IllegalStateException when payment already completed")
    void testCreateCheckoutSession_PaymentAlreadyExists() {
        // Given
        when(orderRepository.findById(testPayment.getOrderId())).thenReturn(Optional.of(testOrder));
        when(paymentRepository.existsByOrderIdAndStatus(testOrder.getId(), Payment.STATUS_SUCCEEDED)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> paymentService.createCheckoutSession(testOrder.getId(), testUser.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment already completed for this order");

        verify(orderRepository, times(1)).findById(testPayment.getOrderId());
        verify(paymentRepository, times(1)).existsByOrderIdAndStatus(testOrder.getId(), Payment.STATUS_SUCCEEDED);
    }

    /*
     * Should return existing Stripe customer ID if user already has one
     */
    @Test
    @DisplayName("Should return existing Stripe customer ID")
    void testGetOrCreateStripeCustomer_ExistingCustomer() {
        // Given
        testUser.setStripeCustomerId("cus_existing_123");
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        // When
        String result = paymentService.getOrCreateStripeCustomer(testUser.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo("cus_existing_123");

        verify(userRepository, times(1)).findById(testUser.getId());
        verify(userRepository, never()).save(testUser);
    }

    /*
     * Should create new Stripe customer when user doesn't have one
     */
    @Test
    @DisplayName("Should create new Stripe customer when user has no customer ID")
    void testGetOrCreateStripeCustomer_CreateNew() throws StripeException {
        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class)) {
            // Given
            testUser.setStripeCustomerId(null);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

            // Mock Stripe Customer
            Customer mockCustomer = mock(Customer.class);
            when(mockCustomer.getId()).thenReturn("cus_new_123");
            customerMock.when(() -> Customer.create(any(CustomerCreateParams.class)))
                    .thenReturn(mockCustomer);

            // When
            String result = paymentService.getOrCreateStripeCustomer(testUser.getId());

            // Then
            assertThat(result).isEqualTo("cus_new_123");

            verify(userRepository, times(1)).findById(testUser.getId());
            verify(userRepository, times(1)).save(testUser);
            assertThat(testUser.getStripeCustomerId()).isEqualTo("cus_new_123");
        }

    }

    /*
     * Should throw UserNotFoundException when user doesn't exist
     */
    @Test
    @DisplayName("Should throw UserNotFoundException when user not found")
    void testGetOrCreateStripeCustomer_UserNotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getOrCreateStripeCustomer(999L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with userId: 999");

        verify(userRepository, times(1)).findById(999L);
    }
}
