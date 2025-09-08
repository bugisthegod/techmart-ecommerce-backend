package com.abel.ecommerce.exception;

import com.abel.ecommerce.utils.ResponseResult;
import com.abel.ecommerce.utils.ResultCode;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle Access Denied exception
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseResult> handleAccessDenied(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                ResultCode.FORBIDDEN.getCode(),
                "Access denied. You don't have permission to perform this action."
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
    }

    /**
     * Handle Authentication Exception
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResponseResult> handleAuthentication(AuthenticationException e) {
        log.warn("Authentication failed: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                ResultCode.UNAUTHORIZED.getCode(),
                "Authentication failed. Please login first."
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    /**
     * Handle validation exception
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<String> handleValidationException(MethodArgumentNotValidException e) {
        String message = Optional.ofNullable(e.getBindingResult().getFieldError())
                .map(FieldError::getDefaultMessage)
                .orElse("Validation error");

        log.warn("Validation error: {}", message);
        return ResponseResult.error(ResultCode.PARAM_NOT_VALID.getCode(), message);
    }


}
