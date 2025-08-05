package com.abel.ecommerce.exception;

public class ProductNotFoundException extends BaseException{
    public ProductNotFoundException(String message) {
        super(404, message);
    }

    public ProductNotFoundException(Long id, String field) {
        super(404, String.format("Product not found with %s: %s", field, id));
    }
}
