package com.abel.ecommerce.service.impl;

import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.entity.SeckillMessage;
import com.abel.ecommerce.exception.InsufficientStockException;
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

@Service
@RequiredArgsConstructor
public class SeckillServiceImpl implements SeckillService {


    private final StringRedisTemplate stringRedisTemplate;

    private final StockService stockService;

    private final OrderService orderService;

    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(SeckillServiceImpl.class);

    @Override
    @Transactional
    public SeckillMessage doSeckill(Long userId, Long productId, int quantity) {

        try {
            Long deductStockStatus = stockService.deductStock(productId, quantity);
            Product product = stockService.findProductById(productId);
            int stock = stockService.getStock(productId);
            if (deductStockStatus != 1 ) throw new InsufficientStockException(product.getName(),stock, quantity);

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

            return  seckillMessage;
        }
        catch (Exception e) {
            stockService.restoreStock(productId,quantity);
            throw new RuntimeException(e);
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
        }
        catch (JsonProcessingException e) {
            log.error("Failed to serialize seckill message: {}",
                    map, e);
            throw new RuntimeException("Failed to create message content", e);

        }
    }
}
