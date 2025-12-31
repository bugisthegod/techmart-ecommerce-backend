package com.abel.ecommerce.service.impl;

import com.abel.ecommerce.dto.response.RefundResponse;
import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.Payment;
import com.abel.ecommerce.exception.OrderNotFoundException;
import com.abel.ecommerce.exception.PaymentNotFoundException;
import com.abel.ecommerce.repository.OrderRepository;
import com.abel.ecommerce.repository.PaymentRepository;
import com.abel.ecommerce.service.RefundService;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public RefundResponse processRefund(Long paymentId, String reason) {
        // Find payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id " + paymentId));

        // Validate payment status
        if (!payment.getStatus().equals(Payment.STATUS_SUCCEEDED)) {
            throw new IllegalStateException("Only succeeded payments can be refunded. Current status: " + payment.getStatus());
        }

        // Validate payment has payment intent ID
        if (payment.getStripePaymentIntentId() == null || payment.getStripePaymentIntentId().isEmpty()) {
            throw new IllegalStateException("Payment does not have a Stripe Payment Intent ID");
        }

        try {
            // Create refund in Stripe
            RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                    .setPaymentIntent(payment.getStripePaymentIntentId());

            if (reason != null && !reason.isEmpty()) {
                paramsBuilder.putMetadata("reason", reason);
            }

            Refund refund = Refund.create(paramsBuilder.build());

            // Update payment status
            payment.setStatus(Payment.STATUS_REFUNDED);
            paymentRepository.save(payment);

            // Update order status to CANCELLED
            Order order = orderRepository.findById(payment.getOrderId())
                    .orElseThrow(() -> new OrderNotFoundException(payment.getOrderId()));
            order.setStatus(Order.STATUS_CANCELLED);
            orderRepository.save(order);

            log.info("Refund processed for payment {}, refund ID: {}", paymentId, refund.getId());

            return new RefundResponse(
                    refund.getId(),
                    payment.getAmount(),
                    refund.getStatus(),
                    "Refund processed successfully"
            );

        } catch (StripeException e) {
            log.error("Failed to create refund for payment {}", paymentId, e);
            throw new RuntimeException("Failed to process refund: " + e.getMessage(), e);
        }
    }
}
