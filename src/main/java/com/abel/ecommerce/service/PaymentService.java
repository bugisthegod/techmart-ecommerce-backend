package com.abel.ecommerce.service;

import com.abel.ecommerce.dto.response.CheckoutResponse;
import com.abel.ecommerce.entity.Payment;

public interface PaymentService {

    /**
     * Create Stripe Checkout session for an order
     * @param orderId Order ID to create checkout for
     * @param userId User ID making the payment
     * @return Checkout response with session ID and URL
     */
    CheckoutResponse createCheckoutSession(Long orderId, Long userId);

    /**
     * Get payment by order ID
     * @param orderId Order ID
     * @return Payment entity
     */
    Payment getPaymentByOrderId(Long orderId);

    /**
     * Update payment status
     * @param paymentId Payment ID
     * @param status New payment status
     * @param stripePaymentIntentId Stripe Payment Intent ID (optional)
     * @return Updated payment entity
     */
    Payment updatePaymentStatus(Long paymentId, Integer status, String stripePaymentIntentId);

    /**
     * Get or create Stripe customer for user
     * @param userId User ID
     * @return Stripe customer ID
     */
    String getOrCreateStripeCustomer(Long userId);

    /**
     * Find payment by Stripe session ID
     * @param sessionId Stripe session ID
     * @return Payment entity
     */
    Payment findByStripeSessionId(String sessionId);
}
