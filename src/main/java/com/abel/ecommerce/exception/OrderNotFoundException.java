package com.abel.ecommerce.exception;

public class OrderNotFoundException extends BaseException {
    
    public OrderNotFoundException(String message) {
        super(404, message);
    }
    
    public OrderNotFoundException(Long orderId) {
        super(404, "Order not found with ID: " + orderId);
    }
    
    public OrderNotFoundException(String orderNo, String field) {
        super(404, String.format("Order not found with %s: %s", field, orderNo));
    }
}
