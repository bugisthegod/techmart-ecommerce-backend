package com.abel.ecommerce.exception;

public class UnauthorizedAccessException extends BaseException {

    public UnauthorizedAccessException(String message) {
        super(403, message);
    }
}
