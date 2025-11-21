package com.abel.ecommerce.init;

import com.abel.ecommerce.constant.RedisKeyConstants;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unchecked", "NullableProblems"})
@Component
public class StockWarmer {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Object> objectRedisTemplate;

    private static final Logger log = LoggerFactory.getLogger(StockWarmer.class);

    @PostConstruct
    public void warmupStart() {
        List<Product> productList = productRepository.findAll();

        stringRedisTemplate.executePipelined(new SessionCallback<Object>() {

            @Override
            public Object execute(RedisOperations operations) {
                for (Product product : productList) {
                    operations.opsForValue().set(RedisKeyConstants.PRODUCT_STOCK_PREFIX + product.getId(), String.valueOf(product.getStock()));
                }
                return null;
            }
        });

        objectRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                for (Product product : productList) {
                    operations.opsForValue().set(RedisKeyConstants.PRODUCT_INFO_PREFIX + product.getId(), product, 1, TimeUnit.HOURS);
                }
                return null;
            }
        });

        log.info("Warmed up {} products", productList.size());
    }

}
