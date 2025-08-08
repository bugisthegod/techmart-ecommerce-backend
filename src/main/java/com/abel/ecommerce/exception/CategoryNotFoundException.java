package com.abel.ecommerce.exception;

public class CategoryNotFoundException extends BaseException {

    public CategoryNotFoundException(String message) {
        super(404, message);
    }

    public CategoryNotFoundException(Long id, String field) {
        super(404, String.format("Category not found with %s: %s", field, id));
    }

}
