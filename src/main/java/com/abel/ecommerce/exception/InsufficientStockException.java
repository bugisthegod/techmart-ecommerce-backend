package com.abel.ecommerce.exception;

public class InsufficientStockException extends BaseException {
    public InsufficientStockException(String message) {
        super(404, message);
    }

    public InsufficientStockException(String productName, Integer available, Integer requested) {
        super(404, String.format("Insufficient stock for product '%s'. Available: %d, Requested: %d",
                productName, available, requested));
    }

}
