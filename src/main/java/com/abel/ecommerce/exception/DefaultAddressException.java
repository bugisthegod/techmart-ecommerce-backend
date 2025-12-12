package com.abel.ecommerce.exception;

public class DefaultAddressException extends BaseException {

    public DefaultAddressException(String message) {
        super(400, message);
    }

}
