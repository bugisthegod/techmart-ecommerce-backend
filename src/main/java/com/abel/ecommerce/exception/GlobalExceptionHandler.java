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
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
    public ResponseEntity<ResponseResult> handleValidationException(MethodArgumentNotValidException e) {
        String message = Optional.ofNullable(e.getBindingResult().getFieldError())
                .map(FieldError::getDefaultMessage)
                .orElse("Validation error");

        log.warn("Validation error: {}", message);

        ResponseResult result = ResponseResult.error(
                ResultCode.PARAM_NOT_VALID.getCode(),
                message
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * Handle type mismatch exception (e.g., passing string when Long is expected)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseResult> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Type mismatch: {}", e.getMessage());

        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                e.getValue(),
                e.getName(),
                e.getRequiredType().getSimpleName());

        ResponseResult result = ResponseResult.error(
                ResultCode.PARAM_NOT_VALID.getCode(),
                message
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * Handle Product Not Found Exception
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ResponseResult> handleProductNotFound(ProductNotFoundException e) {
        log.warn("Product not found: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                ResultCode.PRODUCT_NOT_EXIST.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    /**
     * Handle Category Not Found Exception
     */
    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ResponseResult> handleCategoryNotFound(CategoryNotFoundException e) {
        log.warn("Category not found: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                ResultCode.CATEGORY_NOT_EXIST.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    /**
     * Handle Category Already Exists Exception
     */
    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ResponseEntity<ResponseResult> handleCategoryAlreadyExists(CategoryAlreadyExistsException e) {
        log.warn("Category already exists: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                ResultCode.CATEGORY_ALREADY_EXIST.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }

    /**
     * Handle Address Not Found Exception
     */
    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<ResponseResult> handleAddressNotFound(AddressNotFoundException e) {
        log.warn("Address not found: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                ResultCode.COMMON_FAIL.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    /**
     * Handle Default Address Exception
     */
    @ExceptionHandler(DefaultAddressException.class)
    public ResponseEntity<ResponseResult> handleDefaultAddressException(DefaultAddressException e) {
        log.warn("Default address exception: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                ResultCode.COMMON_FAIL.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * Handle Cart Item Not Found Exception
     */
    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<ResponseResult> handleCartItemNotFound(CartItemNotFoundException e) {
        log.warn("Cart item not found: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                ResultCode.CART_ITEM_NOT_EXIST.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    /**
     * Handle Insufficient Stock Exception
     */
    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ResponseResult> handleInsufficientStock(InsufficientStockException e) {
        log.warn("Insufficient stock: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                ResultCode.PRODUCT_OUT_OF_STOCK.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * Handle User Already Exists Exception
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ResponseResult> handleUserAlreadyExists(UserAlreadyExistsException e) {
        log.warn("User already exists: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                e.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
    }

    /**
     * Handle User Not Found Exception
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ResponseResult> handleUserNotFound(UserNotFoundException e) {
        log.warn("User not found: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                e.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    /**
     * Handle Incorrect Password Exception
     */
    @ExceptionHandler(IncorrectPasswordException.class)
    public ResponseEntity<ResponseResult> handleIncorrectPassword(IncorrectPasswordException e) {
        log.warn("Incorrect password: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                e.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
    }

    /**
     * Handle Order Not Found Exception
     */
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ResponseResult> handleOrderNotFound(OrderNotFoundException e) {
        log.warn("Order not found: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                ResultCode.ORDER_NOT_EXIST.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    /**
     * Handle Order Status Exception
     */
    @ExceptionHandler(OrderStatusException.class)
    public ResponseEntity<ResponseResult> handleOrderStatus(OrderStatusException e) {
        log.warn("Order status error: {}", e.getMessage());

        ResponseResult result = ResponseResult.error(
                ResultCode.ORDER_STATUS_ERROR.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseResult> handleGenericException(Exception e) {
        log.error("Unexpected error occurred", e);

        ResponseResult result = ResponseResult.error(
                ResultCode.COMMON_FAIL.getCode(),
                e.getMessage()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

}
