package com.abel.ecommerce.exception;

public class UserAlreadyExistsException extends BaseException {

    public UserAlreadyExistsException(String message) {
        super(409, message);
    }

    public static UserAlreadyExistsException username(String username) {
        return new UserAlreadyExistsException("Username already exists: " + username);
    }

    public static UserAlreadyExistsException email(String email) {
        return new UserAlreadyExistsException("Email already exists: " + email);
    }

}
