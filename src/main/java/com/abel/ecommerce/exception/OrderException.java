package com.abel.ecommerce.exception;

public class OrderException extends BaseException {

    public OrderException(String message) {
        super(400, message);
    }

    public OrderException(int code, String message) {
        super(code, message);
    }
}
