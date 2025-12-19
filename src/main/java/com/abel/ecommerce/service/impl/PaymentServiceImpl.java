package com.abel.ecommerce.service.impl;

import com.abel.ecommerce.config.StripeConfig;
import com.abel.ecommerce.dto.response.CheckoutResponse;
import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.Payment;
import com.abel.ecommerce.entity.User;
import com.abel.ecommerce.enums.PaymentStatus;
import com.abel.ecommerce.exception.OrderNotFoundException;
import com.abel.ecommerce.exception.OrderStatusException;
import com.abel.ecommerce.exception.PaymentNotFoundException;
import com.abel.ecommerce.exception.UnauthorizedAccessException;
import com.abel.ecommerce.exception.UserNotFoundException;
import com.abel.ecommerce.repository.OrderRepository;
import com.abel.ecommerce.repository.PaymentRepository;
import com.abel.ecommerce.repository.UserRepository;
import com.abel.ecommerce.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final StripeConfig stripeConfig;

    @Override
    @Transactional
    public CheckoutResponse createCheckoutSession(Long orderId, Long userId) {
        // Find and validate order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        // Check if order belongs to user
        if (!order.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("You are not authorized to access this order");
        }

        // Check if order is in pending payment status
        if (!order.isPendingPayment()) {
            throw new OrderStatusException("Order is not in pending payment status");
        }

        // Check if payment already exists
        if (paymentRepository.existsByOrderId(order.getId())) {
            throw new IllegalStateException("Payment already exists for this order");
        }

        try {
            // Get or create Stripe customer
            String customerId = getOrCreateStripeCustomer(userId);

            // Create Checkout Session
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setCustomer(customerId)
                    .setSuccessUrl(stripeConfig.getSuccessUrl())
                    .setCancelUrl(stripeConfig.getCancelUrl())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("usd")
                                                    .setUnitAmount(order.getPayAmount().multiply(new BigDecimal("100")).longValue())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Order #" + order.getOrderNo())
                                                                    .setDescription("Payment for order " + order.getOrderNo())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putMetadata("orderId", order.getId().toString())
                    .putMetadata("userId", userId.toString())
                    .build();

            Session session = Session.create(params);

            // Create payment record
            Payment payment = new Payment();
            payment.setOrderId(order.getId());
            payment.setStripeSessionId(session.getId());
            payment.setAmount(order.getPayAmount());
            payment.setCurrency("USD");
            payment.setStatus(PaymentStatus.PENDING);

            paymentRepository.save(payment);

            log.info("Created Stripe checkout session for order {}: {}", orderId, session.getId());

            return new CheckoutResponse(session.getId(), session.getUrl());

        } catch (StripeException e) {
            log.error("Failed to create Stripe checkout session for order {}", orderId, e);
            throw new RuntimeException("Failed to create checkout session: " + e.getMessage(), e);
        }
    }

    @Override
    public Payment getPaymentByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for order " + orderId));
    }

    @Override
    @Transactional
    public Payment updatePaymentStatus(Long paymentId, PaymentStatus status, String stripePaymentIntentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id " + paymentId));

        payment.setStatus(status);
        if (stripePaymentIntentId != null) {
            payment.setStripePaymentIntentId(stripePaymentIntentId);
        }

        Payment updatedPayment = paymentRepository.save(payment);
        log.info("Updated payment {} status to {}", paymentId, status);

        return updatedPayment;
    }

    @Override
    @Transactional
    public String getOrCreateStripeCustomer(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString(), "id"));

        // Return existing customer if available
        if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isEmpty()) {
            return user.getStripeCustomerId();
        }

        // Create new Stripe customer
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .setName(user.getUsername())
                    .putMetadata("userId", userId.toString())
                    .build();

            Customer customer = Customer.create(params);

            // Save customer ID to user
            user.setStripeCustomerId(customer.getId());
            userRepository.save(user);

            log.info("Created Stripe customer for user {}: {}", userId, customer.getId());

            return customer.getId();

        } catch (StripeException e) {
            log.error("Failed to create Stripe customer for user {}", userId, e);
            throw new RuntimeException("Failed to create Stripe customer: " + e.getMessage(), e);
        }
    }

    @Override
    public Payment findByStripeSessionId(String sessionId) {
        return paymentRepository.findByStripeSessionId(sessionId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for session " + sessionId));
    }
}
