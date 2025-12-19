package com.abel.ecommerce.exception;

public class PaymentNotFoundException extends BaseException {

    public PaymentNotFoundException(String message) {
        super(404, message);
    }

    public PaymentNotFoundException(Long paymentId) {
        super(404, "Payment not found with ID: " + paymentId);
    }
}
