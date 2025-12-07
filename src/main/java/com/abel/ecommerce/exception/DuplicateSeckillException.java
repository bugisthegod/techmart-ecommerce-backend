package com.abel.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class DuplicateSeckillException extends BaseException {

    public DuplicateSeckillException(Long userId, Long productId) {
        super(HttpStatus.CONFLICT.value(),
                String.format("User %d has already participated in seckill for product %d", userId, productId));
    }
}