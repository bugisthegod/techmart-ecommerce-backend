package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by order ID
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * Find payment by Stripe session ID
     */
    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    /**
     * Find payment by Stripe Payment Intent ID
     */
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    /**
     * Check if payment exists for order
     */
    boolean existsByOrderId(Long orderId);

    /**
     * Check if payment exists for order with specific status
     */
    boolean existsByOrderIdAndStatus(Long orderId, Integer status);

    /**
     * Check if payment exists with given session ID
     */
    boolean existsByStripeSessionId(String stripeSessionId);
}
