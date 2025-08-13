package com.abel.ecommerce.exception;

public class OrderStatusException extends BaseException {
    
    public OrderStatusException(String message) {
        super(400, message);
    }
    
    public OrderStatusException(int code, String message) {
        super(code, message);
    }
    
    public static OrderStatusException cannotCancel(String orderNo) {
        return new OrderStatusException("Order " + orderNo + " cannot be cancelled in current status");
    }
    
    public static OrderStatusException cannotShip(String orderNo) {
        return new OrderStatusException("Order " + orderNo + " cannot be shipped in current status");
    }
    
    public static OrderStatusException cannotComplete(String orderNo) {
        return new OrderStatusException("Order " + orderNo + " cannot be completed in current status");
    }
    
    public static OrderStatusException invalidStatusTransition(String from, String to) {
        return new OrderStatusException("Invalid status transition from " + from + " to " + to);
    }
}
