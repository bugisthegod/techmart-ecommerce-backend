package com.abel.ecommerce.controller;

import com.abel.ecommerce.dto.request.CheckoutRequest;
import com.abel.ecommerce.dto.request.RefundRequest;
import com.abel.ecommerce.dto.response.CheckoutResponse;
import com.abel.ecommerce.dto.response.PaymentResponse;
import com.abel.ecommerce.dto.response.RefundResponse;
import com.abel.ecommerce.entity.Payment;
import com.abel.ecommerce.service.PaymentService;
import com.abel.ecommerce.service.RefundService;
import com.abel.ecommerce.utils.ResponseResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Management", description = "Stripe payment checkout and refund operations")
public class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    @Operation(summary = "Create checkout session", description = "Create Stripe Checkout session for an order")
    @PostMapping("/checkout")
    public ResponseResult<CheckoutResponse> createCheckoutSession(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Checkout request") @Valid @RequestBody CheckoutRequest request) {

        CheckoutResponse response = paymentService.createCheckoutSession(request.getOrderId(), userId);
        return ResponseResult.ok(response);
    }

    @Operation(summary = "Get payment by order ID", description = "Retrieve payment information for an order")
    @GetMapping("/order/{orderId}")
    public ResponseResult<PaymentResponse> getPaymentByOrderId(
            @Parameter(description = "User ID") @RequestParam Long userId,
            @Parameter(description = "Order ID") @PathVariable Long orderId) {

        Payment payment = paymentService.getPaymentByOrderId(orderId);

        PaymentResponse response = new PaymentResponse();
        BeanUtils.copyProperties(payment, response);

        return ResponseResult.ok(response);
    }

    @Operation(summary = "Get payment by session ID", description = "Retrieve payment and order information using Stripe session ID")
    @GetMapping("/session/{sessionId}")
    public ResponseResult<PaymentResponse> getPaymentBySessionId(
            @Parameter(description = "Stripe Session ID") @PathVariable String sessionId) {

        Payment payment = paymentService.findByStripeSessionId(sessionId);

        PaymentResponse response = new PaymentResponse();
        BeanUtils.copyProperties(payment, response);

        return ResponseResult.ok(response);
    }

    @Operation(summary = "Process refund", description = "Process refund for a payment (Admin only)")
    @PostMapping("/{paymentId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseResult<RefundResponse> processRefund(
            @Parameter(description = "Payment ID") @PathVariable Long paymentId,
            @Parameter(description = "Refund request") @Valid @RequestBody RefundRequest request) {

        RefundResponse response = refundService.processRefund(paymentId, request.getReason());
        return ResponseResult.ok(response);
    }
}
