package com.abel.ecommerce.exception;

import com.abel.ecommerce.utils.ResultCode;

public class UserAlreadyExistsException extends BaseException {

    public UserAlreadyExistsException(String message) {
        super(409, message);
    }

}
