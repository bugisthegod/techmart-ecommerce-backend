package com.abel.ecommerce.constant;

public class RedisKeyConstants {

    // Product
    public static final String PRODUCT_STOCK_PREFIX = "product:stock:";
    public static final String PRODUCT_INFO_PREFIX = "product:info:";

    // Seckill
    public static final String SECKILL_USER_PREFIX = "seckill:user:";

    //

    private RedisKeyConstants(){
        throw new AssertionError("Cannot instantiate constants class");
    }

    public static String getProductStockKey(Long productId) {
        return PRODUCT_STOCK_PREFIX + productId;
    }

    public static String getProductInfoKey(Long productId) {
        return PRODUCT_INFO_PREFIX + productId;
    }

    public static String getSeckillUserKey(Long userId, Long productId) {
        return SECKILL_USER_PREFIX + productId + ":" + userId;
    }

}
