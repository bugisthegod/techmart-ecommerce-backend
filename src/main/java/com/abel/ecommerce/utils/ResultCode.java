package com.abel.ecommerce.utils;

public enum ResultCode {

    /* Success */
    SUCCESS(200, "Success"),

    /* Default Failure */
    COMMON_FAIL(999, "Operation failed"),

    /* Parameter Errors: 1000～1999 */
    PARAM_NOT_VALID(1001, "Invalid parameter"),
    PARAM_IS_BLANK(1002, "Parameter is blank"),
    PARAM_TYPE_ERROR(1003, "Parameter type error"),
    PARAM_NOT_COMPLETE(1004, "Missing parameter"),

    /* User Errors: 2000～2999 */
    USER_NOT_LOGIN(2001, "User not logged in"),
    USER_ACCOUNT_EXPIRED(2002, "Account expired"),
    USER_CREDENTIALS_ERROR(2003, "Incorrect password"),
    USER_CREDENTIALS_EXPIRED(2004, "Password expired"),
    USER_ACCOUNT_DISABLE(2005, "Account disabled"),
    USER_ACCOUNT_LOCKED(2006, "Account locked"),
    USER_ACCOUNT_NOT_EXIST(2007, "Account does not exist"),
    USER_ACCOUNT_ALREADY_EXIST(2008, "Account already exists"),
    USER_ACCOUNT_USE_BY_OTHERS(2009, "Your login has timed out or you have logged in on another device, you have been forced offline"),

    /* Business Errors: 4000～4999 */
    NO_PERMISSION(4001, "No permission"),

    /* Department Errors: 5000～5999 */
    DEPARTMENT_NOT_EXIST(5007, "Department does not exist"),
    DEPARTMENT_ALREADY_EXIST(5008, "Department already exists"),

    /* Product Errors: 6000～6999 */
    PRODUCT_NOT_EXIST(6001, "Product does not exist"),
    PRODUCT_OUT_OF_STOCK(6002, "Product out of stock"),
    PRODUCT_ALREADY_EXIST(6003, "Product already exists"),

    /* Order Errors: 7000～7999 */
    ORDER_NOT_EXIST(7001, "Order does not exist"),
    ORDER_STATUS_ERROR(7002, "Invalid order status"),
    ORDER_CANNOT_CANCEL(7003, "Order cannot be cancelled"),

    /* Cart Errors: 8000～8999 */
    CART_ITEM_NOT_EXIST(8001, "Cart item does not exist"),
    CART_IS_EMPTY(8002, "Shopping cart is empty"),

    /* Runtime Exceptions: 9000～9999 */
    TOKEN_IS_NULL(9001, "Token cannot be null"),
    TOKEN_INVALID_EXCEPTION(9000, "Token has been tampered with"),
    ARITHMETIC_EXCEPTION(9001, "Arithmetic exception"),
    NULL_POINTER_EXCEPTION(9002, "Null pointer exception"),
    ARRAY_INDEX_OUT_OF_BOUNDS_EXCEPTION(9003, "Array index out of bounds");

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    private final Integer code;
    private final String message;

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}