package com.abel.ecommerce.exception;

import com.abel.ecommerce.utils.ResultCode;

public class UserAlreadyExistsException extends BaseException {

    public UserAlreadyExistsException(String message) {
        super(409, message);
    }

    public UserAlreadyExistsException() {
        super(ResultCode.USER_ACCOUNT_ALREADY_EXIST.getCode(), ResultCode.USER_ACCOUNT_ALREADY_EXIST.getMessage());
    }

    public static UserAlreadyExistsException username(String username) {
        return new UserAlreadyExistsException("Username already exists: " + username);
    }

    public static UserAlreadyExistsException email(String email) {
        return new UserAlreadyExistsException("Email already exists: " + email);
    }

}
