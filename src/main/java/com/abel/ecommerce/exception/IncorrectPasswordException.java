package com.abel.ecommerce.exception;

import com.abel.ecommerce.utils.ResultCode;

public class IncorrectPasswordException extends BaseException {

    public IncorrectPasswordException(String username) {
        super(ResultCode.USER_CREDENTIALS_ERROR.getCode(), 
              "Incorrect password for user: " + username);
    }
}