package com.abel.ecommerce.service.impl;

import com.abel.ecommerce.config.StripeConfig;
import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.Payment;
import com.abel.ecommerce.repository.OrderRepository;
import com.abel.ecommerce.repository.PaymentRepository;
import com.abel.ecommerce.service.OrderService;
import com.abel.ecommerce.service.StripeWebhookService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookServiceImpl implements StripeWebhookService {

    private final StripeConfig stripeConfig;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Override
    public Event processWebhook(String payload, String signature) {
        try {
            Event event = Webhook.constructEvent(
                    payload,
                    signature,
                    stripeConfig.getWebhookSecret()
            );

            log.info("Received Stripe webhook event: {}", event.getType());

            // Handle different event types
            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                case "charge.refunded":
                    handleChargeRefunded(event);
                    break;
                default:
                    log.info("Unhandled event type: {}", event.getType());
            }

            return event;

        } catch (SignatureVerificationException e) {
            log.error("Webhook signature verification failed", e);
            throw new IllegalArgumentException("Invalid signature");
        }
    }

    @Override
    @Transactional
    public void handleCheckoutSessionCompleted(Event event) {
        Session session;
        try {
            session = (Session) event.getData().getObject();
        } catch (Exception e) {
            log.error("Failed to deserialize checkout session from event", e);
            return;
        }

        if (session == null) {
            log.error("Checkout session is null in event");
            return;
        }

        String sessionId = session.getId();
        String paymentIntentId = session.getPaymentIntent();

        log.info("Processing checkout.session.completed for session: {}", sessionId);

        // Find payment by session ID
        Optional<Payment> paymentOpt = paymentRepository.findByStripeSessionId(sessionId);
        if (paymentOpt.isEmpty()) {
            log.error("Payment not found for session: {}", sessionId);
            return;
        }

        Payment payment = paymentOpt.get();

        // Idempotency check
        if (payment.getStatus().equals(Payment.STATUS_SUCCEEDED)) {
            log.info("Payment already processed for session: {}", sessionId);
            return;
        }

        // Update payment status
        payment.setStatus(Payment.STATUS_SUCCEEDED);
        payment.setStripePaymentIntentId(paymentIntentId);
        paymentRepository.save(payment);

        // Update order status to PAID
        Optional<Order> orderOpt = orderRepository.findById(payment.getOrderId());
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setStatus(Order.STATUS_PAID);
            order.setPaymentTime(LocalDateTime.now());
            orderRepository.save(order);
            log.info("Order {} marked as PAID", order.getId());
        } else {
            log.error("Order not found for payment with orderId: {}", payment.getOrderId());
        }
    }

    @Override
    @Transactional
    public void handlePaymentIntentFailed(Event event) {
        com.stripe.model.PaymentIntent paymentIntent;
        try {
            paymentIntent = (com.stripe.model.PaymentIntent) event.getData().getObject();
        } catch (Exception e) {
            log.error("Failed to deserialize payment intent from event", e);
            return;
        }

        if (paymentIntent == null) {
            log.error("Payment intent is null in event");
            return;
        }

        String paymentIntentId = paymentIntent.getId();
        log.info("Processing payment_intent.payment_failed for: {}", paymentIntentId);

        // Find payment by payment intent ID
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntentId);
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for payment intent: {}", paymentIntentId);
            return;
        }

        Payment payment = paymentOpt.get();

        // Update payment status to FAILED
        payment.setStatus(Payment.STATUS_FAILED);
        paymentRepository.save(payment);

        log.info("Payment {} marked as FAILED", payment.getId());
    }

    @Override
    @Transactional
    public void handleChargeRefunded(Event event) {
        com.stripe.model.Charge charge;
        try {
            charge = (com.stripe.model.Charge) event.getData().getObject();
        } catch (Exception e) {
            log.error("Failed to deserialize charge from event", e);
            return;
        }

        if (charge == null) {
            log.error("Charge is null in event");
            return;
        }

        String paymentIntentId = charge.getPaymentIntent();
        log.info("Processing charge.refunded for payment intent: {}", paymentIntentId);

        // Find payment by payment intent ID
        Optional<Payment> paymentOpt = paymentRepository.findByStripePaymentIntentId(paymentIntentId);
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for payment intent: {}", paymentIntentId);
            return;
        }

        Payment payment = paymentOpt.get();

        // Update payment status to REFUNDED (if not already)
        if (!payment.getStatus().equals(Payment.STATUS_REFUNDED)) {
            payment.setStatus(Payment.STATUS_REFUNDED);
            paymentRepository.save(payment);
            log.info("Payment {} marked as REFUNDED via webhook", payment.getId());
        }
    }
}
