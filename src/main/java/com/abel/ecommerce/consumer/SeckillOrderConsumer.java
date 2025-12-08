package com.abel.ecommerce.consumer;

import com.abel.ecommerce.entity.Address;
import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.OrderItem;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.entity.ReliableMessage;
import com.abel.ecommerce.exception.AddressNotFoundException;
import com.abel.ecommerce.exception.BaseException;
import com.abel.ecommerce.exception.ProductNotFoundException;
import com.abel.ecommerce.repository.ReliableMessageRepository;
import com.abel.ecommerce.repository.SeckillMessageRepository;
import com.abel.ecommerce.service.AddressService;
import com.abel.ecommerce.service.OrderService;
import com.abel.ecommerce.service.ProductService;
import com.abel.ecommerce.service.StockService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillOrderConsumer {

    private final OrderService orderService;
    private final ProductService productService;
    private final SeckillMessageRepository seckillMessageRepository;
    private final ObjectMapper objectMapper;
    private final ReliableMessageRepository reliableMessageRepository;
    private final AddressService addressService;
    private final RabbitTemplate rabbitTemplate;
    private final StockService stockService;


    // 1 insert reliable message table to know: is it the first time to write
    // 2 if the first time, continue, no return
    // 3 create order and set status unpaid, and reduce product in database
    // 4 send a message to TTL + dead letter queue

    @RabbitListener(queues = "seckill.order.queue")
    @Transactional
    public void handleSeckillOrder(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageContent = new String(message.getBody());
        log.info("Received seckill order message: {}", messageContent);

        // Step 1: Parse and validate message format (poison message check)
        Map<String, Object> map;
        String orderNo;
        Long userId;
        Long productId;
        int quantity;

        try {
            // Parse JSON
            map = objectMapper.readValue(messageContent, new TypeReference<Map<String, Object>>() {
            });

            // Validate required fields exist
            if (!map.containsKey("orderNo") || !map.containsKey("userId") ||
                    !map.containsKey("productId") || !map.containsKey("quantity")) {
                log.error("Invalid message: missing required fields. Message: {}", messageContent);
                channel.basicAck(deliveryTag, false);  // Discard poison message
                return;
            }

            // Extract and convert values
            orderNo = String.valueOf(map.get("orderNo"));
            userId = ((Number) map.get("userId")).longValue();  // Handle Integer or Long
            productId = ((Number) map.get("productId")).longValue();
            quantity = ((Number) map.get("quantity")).intValue();

        }
        catch (JsonProcessingException e) {
            // Malformed JSON - poison message
            log.error("Failed to parse message JSON: {}", messageContent, e);
            channel.basicAck(deliveryTag, false);  // Discard poison message
            return;
        }
        catch (ClassCastException | NullPointerException e) {
            // Invalid data types - poison message
            log.error("Invalid message format: {}", messageContent, e);
            channel.basicAck(deliveryTag, false);  // Discard poison message
            return;
        }

        // Step 2: Check idempotency (prevent duplicate processing)
        try {
            ReliableMessage reliableMessage = new ReliableMessage();
            reliableMessage.setConsumerName("seckill.order.queue");
            reliableMessage.setMessageId(orderNo);
            reliableMessageRepository.save(reliableMessage);
        }
        catch (DataIntegrityViolationException e) {
            // Duplicate detected - already processed
            log.warn("Duplicate message {} detected, already processed", orderNo);
            channel.basicAck(deliveryTag, false);  // ACK to remove from queue
            return;
        }
        catch (Exception e) {
            // Database error - may be transient, requeue
            log.error("Failed to save reliable message for order: {}. Message will be requeued.",
                    orderNo, e);
            channel.basicNack(deliveryTag, false, true);
            throw new RuntimeException(e);
        }

        // Step 3: Process business logic (create order, deduct stock)
        try {
            Address defaultAddress = addressService.findDefaultAddress(userId);
            Product product = productService.findProductById(productId);
            BigDecimal totalAmount = product.getPrice().multiply(new BigDecimal(quantity));

            // Create order with PENDING_PAYMENT status
            Order order = new Order();
            order.setUserId(userId);
            order.setOrderNo(orderNo);
            order.setTotalAmount(totalAmount);
            order.setPayAmount(totalAmount);
            order.setReceiverAddress(defaultAddress.getFullAddress());
            order.setReceiverPhone(defaultAddress.getReceiverPhone());
            order.setReceiverName(defaultAddress.getReceiverName());
            order.setStatus(Order.STATUS_PENDING_PAYMENT);
            orderService.saveOrder(order);

            // Create order items
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setOrderNo(orderNo);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setQuantity(quantity);
            orderItem.setProductPrice(product.getPrice());
            orderItem.setTotalAmount(totalAmount);
            List<OrderItem> orderItems = new ArrayList<>();
            orderItems.add(orderItem);
            orderService.saveOrderItems(orderItems);

            // Reduce product stock in database (keep in sync with Redis)
            product.setStock(product.getStock() - quantity);
            product.setSales(product.getSales() + quantity);
            productService.updateProduct(product);

            // Step 4: Send payment timeout message (TTL + DLQ)
            try {
                rabbitTemplate.convertAndSend("payment.timeout.exchange", "payment.timeout", orderNo);
                log.info("Payment timeout message sent for order: {}", orderNo);
            }
            catch (Exception e) {
                // Log error but don't fail the order creation
                // Backup scheduled task should catch orders without timeout messages
                log.error("Failed to send payment timeout message for order: {}. " +
                        "Order created successfully but timeout check may not trigger.", orderNo, e);
            }

            // ACK the seckill message (order created successfully)
            channel.basicAck(deliveryTag, false);
            log.info("Seckill order processed successfully. OrderNo: {}, UserId: {}, ProductId: {}, Quantity: {}",
                    orderNo, userId, productId, quantity);

        }
        catch (AddressNotFoundException e) {
            // PERMANENT FAILURE: User has no default address - ACK to discard message
            log.error("PERMANENT FAILURE - No default address for user {}. Order {} will be DISCARDED. " +
                    "User needs to add a default address. Error: {}", userId, orderNo, e.getMessage());

            compensateRedisStock(productId, quantity);

            channel.basicAck(deliveryTag, false);  // ACK to remove from queue permanently
            throw e;  // Rollback transaction to clean up ReliableMessage
        }
        catch (ProductNotFoundException e) {
            // PERMANENT FAILURE: Product doesn't exist - ACK to discard message
            log.error("PERMANENT FAILURE - Product {} not found for order {}. Message will be DISCARDED. Error: {}",
                    productId, orderNo, e.getMessage());

            compensateRedisStock(productId, quantity);

            channel.basicAck(deliveryTag, false);  // ACK to remove from queue permanently
            throw e;  // Rollback transaction to clean up ReliableMessage
        }
        catch (BaseException e) {
            // PERMANENT FAILURE: Business rule violations (409, 400 errors) - ACK to discard
            log.error("PERMANENT FAILURE - Business rule violation for order {}. Message will be DISCARDED. " +
                    "Error: {}", orderNo, e.getMessage());

            compensateRedisStock(productId, quantity);

            channel.basicAck(deliveryTag, false);  // ACK to remove from queue permanently
            throw e;  // Rollback transaction to clean up ReliableMessage
        }
        catch (Exception e) {
            // TRANSIENT FAILURE: Infrastructure errors (DB connection, timeout, etc.) - REQUEUE
            log.error("TRANSIENT FAILURE - Failed to process order {}. Message will be REQUEUED for retry. " +
                            "OrderNo: {}, UserId: {}, ProductId: {}, Quantity: {}, DeliveryTag: {}",
                    orderNo, orderNo, userId, productId, quantity, deliveryTag, e);
            channel.basicNack(deliveryTag, false, true);  // NACK with requeue=true
            throw new RuntimeException(e);
        }
    }

    /**
     * Compensate Redis stock when order creation fails permanently.
     * This ensures Redis and database stock remain consistent.
     *
     * @param productId The product ID
     * @param quantity  The quantity to restore
     */
    private void compensateRedisStock(Long productId, Integer quantity) {
        log.info("Compensating Redis stock for product {}: restoring {} units", productId, quantity);
        stockService.restoreStock(productId, quantity);
    }

}
