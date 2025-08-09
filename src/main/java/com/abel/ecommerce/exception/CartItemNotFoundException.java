package com.abel.ecommerce.exception;

public class CartItemNotFoundException extends BaseException {
    
    public CartItemNotFoundException(Long id, String field) {
        super(404, String.format("Cart Item not found with %s: %s", field, id));
    }
    
    public CartItemNotFoundException(String message) {
        super(404,message);
    }
}
