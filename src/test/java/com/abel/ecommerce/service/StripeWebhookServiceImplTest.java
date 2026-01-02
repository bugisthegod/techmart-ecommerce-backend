package com.abel.ecommerce.service;

import com.abel.ecommerce.config.StripeConfig;
import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.Payment;
import com.abel.ecommerce.repository.OrderRepository;
import com.abel.ecommerce.repository.PaymentRepository;
import com.abel.ecommerce.service.impl.StripeWebhookServiceImpl;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StripeWebhookServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookService Unit Tests")
class StripeWebhookServiceImplTest {

    @Mock
    private StripeConfig stripeConfig;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private StripeWebhookServiceImpl stripeWebhookService;

    private Payment testPayment;
    private Order testOrder;
    private String testPayload;
    private String testSignature;
    private String webhookSecret;

    @BeforeEach
    void setUp() {
        webhookSecret = "whsec_test_secret";
        testPayload = "{\"id\": \"evt_test_123\", \"type\": \"checkout.session.completed\"}";
        testSignature = "test_signature";

        // Setup test payment
        testPayment = new Payment();
        testPayment.setId(1L);
        testPayment.setOrderId(100L);
        testPayment.setStripeSessionId("cs_test_123");
        testPayment.setStripePaymentIntentId(null);
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
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when signature verification fails")
    void testProcessWebhook_InvalidSignature() {
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            // Given
            when(stripeConfig.getWebhookSecret()).thenReturn(webhookSecret);
            webhookMock.when(() -> Webhook.constructEvent(testPayload, testSignature, webhookSecret))
                    .thenThrow(new SignatureVerificationException("Invalid signature", testSignature));

            // When & Then
            assertThatThrownBy(() -> stripeWebhookService.processWebhook(testPayload, testSignature))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid signature");

            webhookMock.verify(() -> Webhook.constructEvent(testPayload, testSignature, webhookSecret), times(1));
        }
    }

    @Test
    @DisplayName("Should process checkout.session.completed event successfully")
    void testProcessWebhook_CheckoutSessionCompleted() {
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            // Given
            Event mockEvent = mock(Event.class);
            Session mockSession = mock(Session.class);
            Event.Data mockData = mock(Event.Data.class);

            when(stripeConfig.getWebhookSecret()).thenReturn(webhookSecret);
            webhookMock.when(() -> Webhook.constructEvent(testPayload, testSignature, webhookSecret))
                    .thenReturn(mockEvent);

            when(mockEvent.getType()).thenReturn("checkout.session.completed");
            when(mockEvent.getData()).thenReturn(mockData);
            when(mockData.getObject()).thenReturn(mockSession);
            when(mockSession.getId()).thenReturn("cs_test_123");
            when(mockSession.getPaymentIntent()).thenReturn("pi_test_123");

            when(paymentRepository.findByStripeSessionId("cs_test_123"))
                    .thenReturn(Optional.of(testPayment));
            when(orderRepository.findById(100L)).thenReturn(Optional.of(testOrder));

            // When
            Event result = stripeWebhookService.processWebhook(testPayload, testSignature);

            // Then
            assertThat(result).isNotNull();
            verify(paymentRepository).save(argThat(payment ->
                    payment.getStatus().equals(Payment.STATUS_SUCCEEDED) &&
                            payment.getStripePaymentIntentId().equals("pi_test_123")
            ));
            verify(orderRepository).save(argThat(order ->
                    order.getStatus().equals(Order.STATUS_PAID) &&
                            order.getPaymentTime() != null
            ));
        }
    }

    @Test
    @DisplayName("Should handle checkout.session.completed when payment already succeeded (idempotency)")
    void testHandleCheckoutSessionCompleted_AlreadyProcessed() {
        // Given
        testPayment.setStatus(Payment.STATUS_SUCCEEDED);
        testPayment.setStripePaymentIntentId("pi_test_123");

        Event mockEvent = mock(Event.class);
        Session mockSession = mock(Session.class);
        Event.Data mockData = mock(Event.Data.class);

        when(mockEvent.getData()).thenReturn(mockData);
        when(mockData.getObject()).thenReturn(mockSession);
        when(mockSession.getId()).thenReturn("cs_test_123");
        when(mockSession.getPaymentIntent()).thenReturn("pi_test_123");

        when(paymentRepository.findByStripeSessionId("cs_test_123"))
                .thenReturn(Optional.of(testPayment));

        // When
        stripeWebhookService.handleCheckoutSessionCompleted(mockEvent);

        // Then - Should not update payment or order
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle checkout.session.completed when payment not found")
    void testHandleCheckoutSessionCompleted_PaymentNotFound() {
        // Given
        Event mockEvent = mock(Event.class);
        Session mockSession = mock(Session.class);
        Event.Data mockData = mock(Event.Data.class);

        when(mockEvent.getData()).thenReturn(mockData);
        when(mockData.getObject()).thenReturn(mockSession);
        when(mockSession.getId()).thenReturn("cs_test_unknown");

        when(paymentRepository.findByStripeSessionId("cs_test_unknown"))
                .thenReturn(Optional.empty());

        // When
        stripeWebhookService.handleCheckoutSessionCompleted(mockEvent);

        // Then - Should not save anything
        verify(paymentRepository, never()).save(any());
        verify(orderRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should handle checkout.session.completed when order not found")
    void testHandleCheckoutSessionCompleted_OrderNotFound() {
        // Given
        Event mockEvent = mock(Event.class);
        Session mockSession = mock(Session.class);
        Event.Data mockData = mock(Event.Data.class);

        when(mockEvent.getData()).thenReturn(mockData);
        when(mockData.getObject()).thenReturn(mockSession);
        when(mockSession.getId()).thenReturn("cs_test_123");
        when(mockSession.getPaymentIntent()).thenReturn("pi_test_123");

        when(paymentRepository.findByStripeSessionId("cs_test_123"))
                .thenReturn(Optional.of(testPayment));
        when(orderRepository.findById(100L)).thenReturn(Optional.empty());

        // When
        stripeWebhookService.handleCheckoutSessionCompleted(mockEvent);

        // Then - Should update payment but not order
        verify(paymentRepository).save(argThat(payment ->
                payment.getStatus().equals(Payment.STATUS_SUCCEEDED)
        ));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle payment_intent.payment_failed event successfully")
    void testHandlePaymentIntentFailed_Success() {
        // Given
        Event mockEvent = mock(Event.class);
        com.stripe.model.PaymentIntent mockPaymentIntent = mock(com.stripe.model.PaymentIntent.class);
        Event.Data mockData = mock(Event.Data.class);

        testPayment.setStripePaymentIntentId("pi_test_123");

        when(mockEvent.getData()).thenReturn(mockData);
        when(mockData.getObject()).thenReturn(mockPaymentIntent);
        when(mockPaymentIntent.getId()).thenReturn("pi_test_123");

        when(paymentRepository.findByStripePaymentIntentId("pi_test_123"))
                .thenReturn(Optional.of(testPayment));

        // When
        stripeWebhookService.handlePaymentIntentFailed(mockEvent);

        // Then
        verify(paymentRepository).save(argThat(payment ->
                payment.getStatus().equals(Payment.STATUS_FAILED)
        ));
    }

    @Test
    @DisplayName("Should handle payment_intent.payment_failed when payment not found")
    void testHandlePaymentIntentFailed_PaymentNotFound() {
        // Given
        Event mockEvent = mock(Event.class);
        com.stripe.model.PaymentIntent mockPaymentIntent = mock(com.stripe.model.PaymentIntent.class);
        Event.Data mockData = mock(Event.Data.class);

        when(mockEvent.getData()).thenReturn(mockData);
        when(mockData.getObject()).thenReturn(mockPaymentIntent);
        when(mockPaymentIntent.getId()).thenReturn("pi_test_unknown");

        when(paymentRepository.findByStripePaymentIntentId("pi_test_unknown"))
                .thenReturn(Optional.empty());

        // When
        stripeWebhookService.handlePaymentIntentFailed(mockEvent);

        // Then
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle charge.refunded event successfully")
    void testHandleChargeRefunded_Success() {
        // Given
        Event mockEvent = mock(Event.class);
        com.stripe.model.Charge mockCharge = mock(com.stripe.model.Charge.class);
        Event.Data mockData = mock(Event.Data.class);

        testPayment.setStripePaymentIntentId("pi_test_123");
        testPayment.setStatus(Payment.STATUS_SUCCEEDED);

        when(mockEvent.getData()).thenReturn(mockData);
        when(mockData.getObject()).thenReturn(mockCharge);
        when(mockCharge.getPaymentIntent()).thenReturn("pi_test_123");

        when(paymentRepository.findByStripePaymentIntentId("pi_test_123"))
                .thenReturn(Optional.of(testPayment));

        // When
        stripeWebhookService.handleChargeRefunded(mockEvent);

        // Then
        verify(paymentRepository).save(argThat(payment ->
                payment.getStatus().equals(Payment.STATUS_REFUNDED)
        ));
    }

    @Test
    @DisplayName("Should not update payment when already refunded (idempotency)")
    void testHandleChargeRefunded_AlreadyRefunded() {
        // Given
        Event mockEvent = mock(Event.class);
        com.stripe.model.Charge mockCharge = mock(com.stripe.model.Charge.class);
        Event.Data mockData = mock(Event.Data.class);

        testPayment.setStripePaymentIntentId("pi_test_123");
        testPayment.setStatus(Payment.STATUS_REFUNDED);

        when(mockEvent.getData()).thenReturn(mockData);
        when(mockData.getObject()).thenReturn(mockCharge);
        when(mockCharge.getPaymentIntent()).thenReturn("pi_test_123");

        when(paymentRepository.findByStripePaymentIntentId("pi_test_123"))
                .thenReturn(Optional.of(testPayment));

        // When
        stripeWebhookService.handleChargeRefunded(mockEvent);

        // Then
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle charge.refunded when payment not found")
    void testHandleChargeRefunded_PaymentNotFound() {
        // Given
        Event mockEvent = mock(Event.class);
        com.stripe.model.Charge mockCharge = mock(com.stripe.model.Charge.class);
        Event.Data mockData = mock(Event.Data.class);

        when(mockEvent.getData()).thenReturn(mockData);
        when(mockData.getObject()).thenReturn(mockCharge);
        when(mockCharge.getPaymentIntent()).thenReturn("pi_test_unknown");

        when(paymentRepository.findByStripePaymentIntentId("pi_test_unknown"))
                .thenReturn(Optional.empty());

        // When
        stripeWebhookService.handleChargeRefunded(mockEvent);

        // Then
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle unknown event type gracefully")
    void testProcessWebhook_UnknownEventType() {
        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            // Given
            Event mockEvent = mock(Event.class);

            when(stripeConfig.getWebhookSecret()).thenReturn(webhookSecret);
            webhookMock.when(() -> Webhook.constructEvent(testPayload, testSignature, webhookSecret))
                    .thenReturn(mockEvent);

            when(mockEvent.getType()).thenReturn("unknown.event.type");

            // When
            Event result = stripeWebhookService.processWebhook(testPayload, testSignature);

            // Then
            assertThat(result).isNotNull();
            verify(paymentRepository, never()).save(any());
            verify(orderRepository, never()).save(any());
        }
    }

    @Test
    @DisplayName("Should handle null session in checkout.session.completed event")
    void testHandleCheckoutSessionCompleted_NullSession() {
        // Given
        Event mockEvent = mock(Event.class);
        Event.Data mockData = mock(Event.Data.class);

        when(mockEvent.getData()).thenReturn(mockData);
        when(mockData.getObject()).thenReturn(null);

        // When
        stripeWebhookService.handleCheckoutSessionCompleted(mockEvent);

        // Then
        verify(paymentRepository, never()).findByStripeSessionId(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle null payment intent in payment_intent.payment_failed event")
    void testHandlePaymentIntentFailed_NullPaymentIntent() {
        // Given
        Event mockEvent = mock(Event.class);
        Event.Data mockData = mock(Event.Data.class);

        when(mockEvent.getData()).thenReturn(mockData);
        when(mockData.getObject()).thenReturn(null);

        // When
        stripeWebhookService.handlePaymentIntentFailed(mockEvent);

        // Then
        verify(paymentRepository, never()).findByStripePaymentIntentId(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle null charge in charge.refunded event")
    void testHandleChargeRefunded_NullCharge() {
        // Given
        Event mockEvent = mock(Event.class);
        Event.Data mockData = mock(Event.Data.class);

        when(mockEvent.getData()).thenReturn(mockData);
        when(mockData.getObject()).thenReturn(null);

        // When
        stripeWebhookService.handleChargeRefunded(mockEvent);

        // Then
        verify(paymentRepository, never()).findByStripePaymentIntentId(any());
        verify(paymentRepository, never()).save(any());
    }
}