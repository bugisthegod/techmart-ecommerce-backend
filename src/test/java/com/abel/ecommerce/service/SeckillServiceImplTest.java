package com.abel.ecommerce.service;

import com.abel.ecommerce.constant.RedisKeyConstants;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.entity.SeckillMessage;
import com.abel.ecommerce.exception.DuplicateSeckillException;
import com.abel.ecommerce.exception.InsufficientStockException;
import com.abel.ecommerce.repository.SeckillMessageRepository;
import com.abel.ecommerce.service.impl.SeckillServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SeckillServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SeckillService Unit Tests")
class SeckillServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private StockService stockService;

    @Mock
    private OrderService orderService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SeckillMessageRepository seckillMessageRepository;

    @InjectMocks
    private SeckillServiceImpl seckillService;

    private Long testUserId;
    private Long testProductId;
    private int testQuantity;
    private Product testProduct;
    private String testOrderNo;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testProductId = 100L;
        testQuantity = 1;
        testOrderNo = "ORD20231215001";

        testProduct = new Product();
        testProduct.setId(testProductId);
        testProduct.setName("Test Seckill Product");
        testProduct.setPrice(new BigDecimal("99.99"));
        testProduct.setStock(10);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("Should successfully complete seckill when all conditions are met")
    void testDoSeckill_Success() throws Exception {
        // Given
        String userKey = RedisKeyConstants.getSeckillUserKey(testUserId, testProductId);
        String messageJson = "{\"orderNo\":\"" + testOrderNo + "\",\"userId\":1,\"productId\":100}";

        when(valueOperations.setIfAbsent(eq(userKey), anyString(), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true);
        when(stockService.deductStock(testProductId, testQuantity)).thenReturn(1L);
        when(orderService.generateOrderNo(testUserId)).thenReturn(testOrderNo);
        when(objectMapper.writeValueAsString(any())).thenReturn(messageJson);

        SeckillMessage savedMessage = new SeckillMessage();
        savedMessage.setId(1L);
        savedMessage.setOrderNo(testOrderNo);
        savedMessage.setUserId(testUserId);
        savedMessage.setProductId(testProductId);
        when(seckillMessageRepository.save(any(SeckillMessage.class))).thenReturn(savedMessage);

        // When
        SeckillMessage result = seckillService.doSeckill(testUserId, testProductId, testQuantity);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getOrderNo()).isEqualTo(testOrderNo);
        assertThat(result.getUserId()).isEqualTo(testUserId);
        assertThat(result.getProductId()).isEqualTo(testProductId);

        verify(valueOperations).setIfAbsent(eq(userKey), anyString(), eq(24L), eq(TimeUnit.HOURS));
        verify(stockService).deductStock(testProductId, testQuantity);
        verify(orderService).generateOrderNo(testUserId);
        verify(seckillMessageRepository).save(any(SeckillMessage.class));
        verify(stringRedisTemplate, never()).delete(userKey);
        verify(stockService, never()).restoreStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Should throw DuplicateSeckillException when user already participated")
    void testDoSeckill_UserAlreadyParticipated() {
        // Given
        String userKey = RedisKeyConstants.getSeckillUserKey(testUserId, testProductId);

        when(valueOperations.setIfAbsent(eq(userKey), anyString(), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> seckillService.doSeckill(testUserId, testProductId, testQuantity))
                .isInstanceOf(DuplicateSeckillException.class);

        verify(valueOperations).setIfAbsent(eq(userKey), anyString(), eq(24L), eq(TimeUnit.HOURS));
        verify(stockService, never()).deductStock(anyLong(), anyInt());
        verify(seckillMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when stock deduction fails")
    void testDoSeckill_InsufficientStock() {
        // Given
        String userKey = RedisKeyConstants.getSeckillUserKey(testUserId, testProductId);

        when(valueOperations.setIfAbsent(eq(userKey), anyString(), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true);
        when(stockService.deductStock(testProductId, testQuantity)).thenReturn(0L);
        when(stockService.getStock(testProductId)).thenReturn(0);
        when(stockService.findProductById(testProductId)).thenReturn(testProduct);

        // When & Then
        assertThatThrownBy(() -> seckillService.doSeckill(testUserId, testProductId, testQuantity))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining(testProduct.getName());

        verify(valueOperations).setIfAbsent(eq(userKey), anyString(), eq(24L), eq(TimeUnit.HOURS));
        verify(stockService).deductStock(testProductId, testQuantity);
        verify(stringRedisTemplate).delete(userKey);
        verify(seckillMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should restore stock and remove user key when DB save fails")
    void testDoSeckill_DBSaveFailure() throws Exception {
        // Given
        String userKey = RedisKeyConstants.getSeckillUserKey(testUserId, testProductId);
        String messageJson = "{\"orderNo\":\"" + testOrderNo + "\",\"userId\":1,\"productId\":100}";

        when(valueOperations.setIfAbsent(eq(userKey), anyString(), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true);
        when(stockService.deductStock(testProductId, testQuantity)).thenReturn(1L);
        when(orderService.generateOrderNo(testUserId)).thenReturn(testOrderNo);
        when(objectMapper.writeValueAsString(any())).thenReturn(messageJson);
        when(seckillMessageRepository.save(any(SeckillMessage.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then
        assertThatThrownBy(() -> seckillService.doSeckill(testUserId, testProductId, testQuantity))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create seckill message");

        verify(stockService).deductStock(testProductId, testQuantity);
        verify(stockService).restoreStock(testProductId, testQuantity);
        verify(stringRedisTemplate).delete(userKey);
        verify(seckillMessageRepository).save(any(SeckillMessage.class));
    }

    @Test
    @DisplayName("Should restore stock when order number generation fails")
    void testDoSeckill_OrderNoGenerationFailure() throws Exception {
        // Given
        String userKey = RedisKeyConstants.getSeckillUserKey(testUserId, testProductId);

        when(valueOperations.setIfAbsent(eq(userKey), anyString(), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true);
        when(stockService.deductStock(testProductId, testQuantity)).thenReturn(1L);
        when(orderService.generateOrderNo(testUserId))
                .thenThrow(new RuntimeException("Failed to generate order number"));

        // When & Then
        assertThatThrownBy(() -> seckillService.doSeckill(testUserId, testProductId, testQuantity))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create seckill message");

        verify(stockService).deductStock(testProductId, testQuantity);
        verify(stockService).restoreStock(testProductId, testQuantity);
        verify(stringRedisTemplate).delete(userKey);
        verify(seckillMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle JSON serialization failure gracefully")
    void testDoSeckill_JsonSerializationFailure() throws Exception {
        // Given
        String userKey = RedisKeyConstants.getSeckillUserKey(testUserId, testProductId);

        when(valueOperations.setIfAbsent(eq(userKey), anyString(), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true);
        when(stockService.deductStock(testProductId, testQuantity)).thenReturn(1L);
        when(orderService.generateOrderNo(testUserId)).thenReturn(testOrderNo);
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Failed to create message content"));

        // When & Then
        assertThatThrownBy(() -> seckillService.doSeckill(testUserId, testProductId, testQuantity))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create seckill message");

        verify(stockService).deductStock(testProductId, testQuantity);
        verify(stockService).restoreStock(testProductId, testQuantity);
        verify(stringRedisTemplate).delete(userKey);
        verify(seckillMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should verify seckill message content is saved correctly")
    void testDoSeckill_VerifyMessageContent() throws Exception {
        // Given
        String userKey = RedisKeyConstants.getSeckillUserKey(testUserId, testProductId);
        String messageJson = "{\"orderNo\":\"" + testOrderNo + "\",\"userId\":1,\"productId\":100,\"quantity\":1}";

        when(valueOperations.setIfAbsent(eq(userKey), anyString(), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true);
        when(stockService.deductStock(testProductId, testQuantity)).thenReturn(1L);
        when(orderService.generateOrderNo(testUserId)).thenReturn(testOrderNo);
        when(objectMapper.writeValueAsString(any())).thenReturn(messageJson);

        SeckillMessage savedMessage = new SeckillMessage();
        savedMessage.setId(1L);
        when(seckillMessageRepository.save(any(SeckillMessage.class))).thenReturn(savedMessage);

        // When
        seckillService.doSeckill(testUserId, testProductId, testQuantity);

        // Then
        verify(seckillMessageRepository).save(argThat(message ->
                message.getOrderNo().equals(testOrderNo) &&
                        message.getUserId().equals(testUserId) &&
                        message.getProductId().equals(testProductId) &&
                        message.getExchange().equals("seckill.exchange") &&
                        message.getRoutingKey().equals("seckill.order") &&
                        message.getStatus().equals(SeckillMessage.STATUS_PENDING) &&
                        message.getMessageContent().equals(messageJson) &&
                        message.getNextRetryTime() != null
        ));
    }

    @Test
    @DisplayName("Should handle multiple quantity correctly")
    void testDoSeckill_MultipleQuantity() throws Exception {
        // Given
        int multipleQuantity = 5;
        String userKey = RedisKeyConstants.getSeckillUserKey(testUserId, testProductId);
        String messageJson = "{\"orderNo\":\"" + testOrderNo + "\",\"userId\":1,\"productId\":100}";

        when(valueOperations.setIfAbsent(eq(userKey), anyString(), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true);
        when(stockService.deductStock(testProductId, multipleQuantity)).thenReturn(1L);
        when(orderService.generateOrderNo(testUserId)).thenReturn(testOrderNo);
        when(objectMapper.writeValueAsString(any())).thenReturn(messageJson);

        SeckillMessage savedMessage = new SeckillMessage();
        savedMessage.setId(1L);
        when(seckillMessageRepository.save(any(SeckillMessage.class))).thenReturn(savedMessage);

        // When
        SeckillMessage result = seckillService.doSeckill(testUserId, testProductId, multipleQuantity);

        // Then
        assertThat(result).isNotNull();
        verify(stockService).deductStock(testProductId, multipleQuantity);
    }

    @Test
    @DisplayName("Should verify stock deduction returns non-success status code")
    void testDoSeckill_StockDeductionNonSuccess() {
        // Given
        String userKey = RedisKeyConstants.getSeckillUserKey(testUserId, testProductId);

        when(valueOperations.setIfAbsent(eq(userKey), anyString(), eq(24L), eq(TimeUnit.HOURS)))
                .thenReturn(true);
        when(stockService.deductStock(testProductId, testQuantity)).thenReturn(-1L);
        when(stockService.getStock(testProductId)).thenReturn(0);
        when(stockService.findProductById(testProductId)).thenReturn(testProduct);

        // When & Then
        assertThatThrownBy(() -> seckillService.doSeckill(testUserId, testProductId, testQuantity))
                .isInstanceOf(InsufficientStockException.class);

        verify(stringRedisTemplate).delete(userKey);
        verify(seckillMessageRepository, never()).save(any());
    }
}
