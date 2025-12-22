package com.abel.ecommerce.service.impl;

import com.abel.ecommerce.constant.RedisKeyConstants;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.entity.SeckillMessage;
import com.abel.ecommerce.exception.DuplicateSeckillException;
import com.abel.ecommerce.exception.InsufficientStockException;
import com.abel.ecommerce.repository.SeckillMessageRepository;
import com.abel.ecommerce.service.OrderService;
import com.abel.ecommerce.service.SeckillService;
import com.abel.ecommerce.service.StockService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {


    private final StringRedisTemplate stringRedisTemplate;

    private final StockService stockService;

    private final OrderService orderService;

    private final ObjectMapper objectMapper;

    private final SeckillMessageRepository seckillMessageRepository;

    private static final Logger log = LoggerFactory.getLogger(SeckillServiceImpl.class);

    @Override
    public SeckillMessage doSeckill(Long userId, Long productId, int quantity) {

        // 1. Check if user has already participated in this seckill (idempotency check)
        String userKey = RedisKeyConstants.getSeckillUserKey(userId, productId);
        Boolean alreadyParticipated = stringRedisTemplate.opsForValue().setIfAbsent(
                userKey,
                String.valueOf(System.currentTimeMillis()),
                24,
                TimeUnit.HOURS
        );

        // If setIfAbsent returns false, key already exists = user already participated
        if (Boolean.FALSE.equals(alreadyParticipated)) {
            log.warn("User {} already participated in seckill for product {}", userId, productId);
            throw new DuplicateSeckillException(userId, productId);
        }

        // 2. Atomically deduct stock from Redis
        Long deductStockStatus = stockService.deductStock(productId, quantity);

        // 3. If stock deduction failed, remove user key and throw exception
        if (deductStockStatus != 1) {
            // Clean up user participation record since seckill failed
            stringRedisTemplate.delete(userKey);

            int stock = stockService.getStock(productId);
            Product product = stockService.findProductById(productId);
            throw new InsufficientStockException(product.getName(), stock, quantity);
        }

        // 4. Stock deduction succeeded, now save message to DB
        try {
            String orderNo = orderService.generateOrderNo(userId);

            SeckillMessage seckillMessage = new SeckillMessage();
            seckillMessage.setOrderNo(orderNo);
            seckillMessage.setUserId(userId);
            seckillMessage.setProductId(productId);
            seckillMessage.setExchange("seckill.exchange");
            seckillMessage.setRoutingKey("seckill.order");
            seckillMessage.setStatus(SeckillMessage.STATUS_PENDING);
            seckillMessage.setNextRetryTime(LocalDateTime.now());
            seckillMessage.setMessageContent(buildMessageJson(orderNo, userId, productId));
            seckillMessageRepository.save(seckillMessage);

            log.info("User {} successfully participated in seckill for product {}", userId, productId);
            return seckillMessage;
        } catch (Exception e) {
            // Only restore stock if DB save failed AFTER successful stock deduction
            log.error("Failed to save seckill message for product {}, restoring stock and user key", productId, e);
            stockService.restoreStock(productId, quantity);
            stringRedisTemplate.delete(userKey); // Also remove user participation record
            throw new RuntimeException("Failed to create seckill message", e);
        }
    }


    private String buildMessageJson(String orderNo, Long userId, Long productId) {
        Map<String, Object> map = new HashMap<>();
        map.put("orderNo", orderNo);
        map.put("userId", userId);
        map.put("productId", productId);
        map.put("quantity", 1);
        map.put("createTime", System.currentTimeMillis());
        try {
            return objectMapper.writeValueAsString(map);  // 用fastjson或jackson
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize seckill message: {}",
                    map, e);
            throw new RuntimeException("Failed to create message content", e);

        }
    }
}
