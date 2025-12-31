package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("PaymentRepository Tests")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Payment testPayment;

    @BeforeEach
    void setUp() {
        // Create test payment data
        testPayment = new Payment();
        testPayment.setOrderId(1001L);
        testPayment.setStripeSessionId("cs_test_123456");
        testPayment.setStripePaymentIntentId("pi_test_123456");
        testPayment.setAmount(new BigDecimal("99.99"));
        testPayment.setCurrency("USD");
        testPayment.setStatus(Payment.STATUS_PENDING);

        // Persist test data
        entityManager.persist(testPayment);
        entityManager.flush();
    }

    @Test
    @DisplayName("Should find payment by order ID")
    void testFindByOrderId_Success() {
        // When
        Optional<Payment> result = paymentRepository.findByOrderId(1001L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getOrderId()).isEqualTo(1001L);
        assertThat(result.get().getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("Should return empty when order ID not found")
    void testFindByOrderId_NotFound() {
        // When
        Optional<Payment> result = paymentRepository.findByOrderId(9999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find payment by Stripe session ID")
    void testFindByStripeSessionId_Success() {
        // When
        Optional<Payment> result = paymentRepository.findByStripeSessionId("cs_test_123456");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStripeSessionId()).isEqualTo("cs_test_123456");
        assertThat(result.get().getOrderId()).isEqualTo(1001L);
    }

    @Test
    @DisplayName("Should return empty when Stripe session ID not found")
    void testFindByStripeSessionId_NotFound() {
        // When
        Optional<Payment> result = paymentRepository.findByStripeSessionId("cs_test_nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find payment by Stripe Payment Intent ID")
    void testFindByStripePaymentIntentId_Success() {
        // When
        Optional<Payment> result = paymentRepository.findByStripePaymentIntentId("pi_test_123456");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStripePaymentIntentId()).isEqualTo("pi_test_123456");
        assertThat(result.get().getStatus()).isEqualTo(Payment.STATUS_PENDING);
    }

    @Test
    @DisplayName("Should return empty when Stripe Payment Intent ID not found")
    void testFindByStripePaymentIntentId_NotFound() {
        // When
        Optional<Payment> result = paymentRepository.findByStripePaymentIntentId("pi_test_nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return true when payment exists for order ID")
    void testExistsByOrderId_True() {
        // When
        boolean exists = paymentRepository.existsByOrderId(1001L);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when payment does not exist for order ID")
    void testExistsByOrderId_False() {
        // When
        boolean exists = paymentRepository.existsByOrderId(9999L);

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return true when payment exists with Stripe session ID")
    void testExistsByStripeSessionId_True() {
        // When
        boolean exists = paymentRepository.existsByStripeSessionId("cs_test_123456");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when payment does not exist with Stripe session ID")
    void testExistsByStripeSessionId_False() {
        // When
        boolean exists = paymentRepository.existsByStripeSessionId("cs_test_nonexistent");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should handle multiple payments with different statuses")
    void testMultiplePayments() {
        // Given - Create additional payment with different status
        Payment completedPayment = new Payment();
        completedPayment.setOrderId(1002L);
        completedPayment.setStripeSessionId("cs_test_789012");
        completedPayment.setStripePaymentIntentId("pi_test_789012");
        completedPayment.setAmount(new BigDecimal("149.99"));
        completedPayment.setCurrency("USD");
        completedPayment.setStatus(Payment.STATUS_SUCCEEDED);

        entityManager.persist(completedPayment);
        entityManager.flush();

        // When
        Optional<Payment> pendingPayment = paymentRepository.findByOrderId(1001L);
        Optional<Payment> completed = paymentRepository.findByOrderId(1002L);

        // Then
        assertThat(pendingPayment).isPresent();
        assertThat(pendingPayment.get().getStatus()).isEqualTo(Payment.STATUS_PENDING);

        assertThat(completed).isPresent();
        assertThat(completed.get().getStatus()).isEqualTo(Payment.STATUS_SUCCEEDED);
    }

    @Test
    @DisplayName("Should handle null Stripe IDs gracefully")
    void testNullStripeIds() {
        // Given - Create payment without Stripe IDs
        Payment paymentWithoutStripe = new Payment();
        paymentWithoutStripe.setOrderId(1003L);
        paymentWithoutStripe.setStripeSessionId(null);
        paymentWithoutStripe.setStripePaymentIntentId(null);
        paymentWithoutStripe.setAmount(new BigDecimal("49.99"));
        paymentWithoutStripe.setCurrency("USD");
        paymentWithoutStripe.setStatus(Payment.STATUS_PENDING);

        entityManager.persist(paymentWithoutStripe);
        entityManager.flush();

        // When
        Optional<Payment> result = paymentRepository.findByOrderId(1003L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getStripeSessionId()).isNull();
        assertThat(result.get().getStripePaymentIntentId()).isNull();
    }
}
