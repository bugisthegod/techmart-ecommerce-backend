package com.abel.ecommerce.service.impl;

import com.abel.ecommerce.constant.RedisKeyConstants;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.exception.ProductNotFoundException;
import com.abel.ecommerce.repository.ProductRepository;
import com.abel.ecommerce.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StringRedisTemplate stringRedisTemplate;

    private final RedisTemplate<String, Object> objectRedisTemplate;

    private final ProductRepository productRepository;


    private static final String DEDUCT_STOCK_SCRIPT =
            "local stock = redis.call('GET', KEYS[1]) " +
                    "if stock == false then " +
                    "  return -1 " +
                    "end " +
                    "stock = tonumber(stock) " +
                    "if stock >= tonumber(ARGV[1]) then " +
                    "  redis.call('DECRBY', KEYS[1], ARGV[1]) " +
                    "  return 1 " +
                    "else " +
                    "  return 0 " +
                    "end";

    public Long deductStock(Long productId, Integer quantity) {
        String key = RedisKeyConstants.getProductStockKey(productId);

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(DEDUCT_STOCK_SCRIPT);
        script.setResultType(Long.class);

        return stringRedisTemplate.execute(
                script,
                Collections.singletonList(key),  // KEYS
                String.valueOf(quantity)          // ARGV
        );
    }

    public void restoreStock(Long productId, Integer quantity) {
        String key = RedisKeyConstants.getProductStockKey(productId);
        stringRedisTemplate.opsForValue().increment(key, quantity);
    }

    public int getStock(Long productId) {
        String key = RedisKeyConstants.getProductStockKey(productId);
        String stockStr = stringRedisTemplate.opsForValue().get(key);
        return stockStr != null ? Integer.parseInt(stockStr) : 0;
    }

    @Override
    public Product findProductById(Long id) {
        // TODO: remove redis logic
        String infoKey = RedisKeyConstants.PRODUCT_INFO_PREFIX + id;
        Product product = (Product) objectRedisTemplate.opsForValue().get(infoKey);

        if (product == null) {
            Product productFromDB = productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id, "ID"));
            objectRedisTemplate.opsForValue().set(infoKey, productFromDB, 1 , TimeUnit.HOURS);
            return productFromDB;
        }
        return product;
    }

}
