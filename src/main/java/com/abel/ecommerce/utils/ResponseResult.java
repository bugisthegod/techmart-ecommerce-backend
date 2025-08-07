package com.abel.ecommerce.utils;

import lombok.Data;

@Data
public class ResponseResult<T> {

    private int status;
    private String msg;
    private T data;

    public ResponseResult() {
    }

    public ResponseResult(int status, String msg) {
        this.status = status;
        this.msg = msg;
    }

    public ResponseResult(T data, String msg, int status) {
        this.status = status;
        this.msg = msg;
        this.data = data;
    }

    public static final ResponseResult<Void> SUCCESS = new ResponseResult<>(200, "Success");
    public static final ResponseResult<Void> INTERNAL_ERROR = new ResponseResult<>(500, "Server error");
    public static final ResponseResult<Void> NOT_FOUND = new ResponseResult<>(404, "Not found");

    public static ResponseResult<Void> ok() {
        return new ResponseResult<>(200, "Success");
    }

    public static <T> ResponseResult<T> ok(T data) {
        return new ResponseResult<>(data, "Success", 200);
    }

    public static <T> ResponseResult<T> ok(T data, String msg) {
        return new ResponseResult<>(data, msg, 200);
    }

    public static <T> ResponseResult<T> ok(ResultCode resultCode) {
        return new ResponseResult<>(resultCode.getCode(), resultCode.getMessage());
    }

    public static <T> ResponseResult<T> error(int status, String msg) {
        return new ResponseResult<>(status, msg);
    }

    public static <T> ResponseResult<T> error(ResultCode resultCode) {
        return new ResponseResult<>(resultCode.getCode(), resultCode.getMessage());
    }
}