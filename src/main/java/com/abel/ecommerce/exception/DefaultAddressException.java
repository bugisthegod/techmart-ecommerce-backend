package com.abel.ecommerce.exception;

public class DefaultAddressException extends BaseException {
    
    public DefaultAddressException(String message) {
        super(400, message);
    }
    
    public DefaultAddressException(int code, String message) {
        super(code, message);
    }
    
    public static DefaultAddressException cannotDeleteLastAddress() {
        return new DefaultAddressException(400, "Cannot delete the last remaining address");
    }
    
    public static DefaultAddressException cannotDeleteDefaultAddress() {
        return new DefaultAddressException(400, "Cannot delete default address. Please set another address as default first");
    }
}
