package com.abel.ecommerce.exception;

public class CategoryAlreadyExistsException extends BaseException {

    public CategoryAlreadyExistsException(String name) {
        super(409, "Category already exists with name: " + name);
    }

}
