package com.abel.ecommerce.exception;

import com.abel.ecommerce.utils.ResultCode;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException(String message) {
        super(404, message);
    }

    public UserNotFoundException(String username, String field) {
        super(404, String.format("User not found with %s: %s", field, username));
    }

}
