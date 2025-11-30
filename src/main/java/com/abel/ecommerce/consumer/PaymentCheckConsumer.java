package com.abel.ecommerce.consumer;

import com.abel.ecommerce.entity.Order;
import com.abel.ecommerce.entity.OrderItem;
import com.abel.ecommerce.entity.Product;
import com.abel.ecommerce.service.OrderService;
import com.abel.ecommerce.service.ProductService;
import com.abel.ecommerce.service.StockService;
import com.rabbitmq.client.Channel;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;


@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCheckConsumer {

    private final OrderService orderService;
    private final StockService stockService;
    private final ProductService productService;


    @RabbitListener(queues = "payment.check.queue")
    @Transactional
    public void checkOrderPayment(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String messageContent = new String(message.getBody());
        log.info("Received order payment status check OrderNo: {}", messageContent);

        String orderNo = messageContent;

        try {
            // Find the order
            Order order = orderService.findOrderByOrderNo(orderNo);

            // Check if order exists
            if (order == null) {
                log.error("Order not found for payment check: {}", orderNo);
                channel.basicAck(deliveryTag, false);  // ACK to remove message
                return;
            }

            // Check if order is still unpaid
            if (Order.STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
                log.warn("Order {} not paid after 15 minutes. Cancelling order and restoring stock.", orderNo);

                // Cancel the order
                order.setStatus(Order.STATUS_CANCELLED);
                orderService.updateOrder(order);

                // Restore stock for all order items
                List<OrderItem> orderItemList = orderService.findOrderItemsByOrderNo(orderNo);
                for (OrderItem orderItem : orderItemList) {
                    // Restore Redis stock
                    stockService.restoreStock(orderItem.getProductId(), orderItem.getQuantity());

                    // Restore database stock
                    Product product = productService.findProductById(orderItem.getProductId());
                    product.setStock(product.getStock() + orderItem.getQuantity());
                    productService.updateProduct(product);

                    log.info("Restored stock for product {}: {} units", orderItem.getProductId(), orderItem.getQuantity());
                }

                log.info("Order {} cancelled successfully due to payment timeout", orderNo);
            } else {
                // Order was paid or already cancelled
                log.info("Order {} status is {}, no action needed", orderNo, order.getStatus());
            }

            // ACK message after successful processing
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            // Business logic error - may be transient (DB connection, etc.), requeue for retry
            log.error("Failed to process payment check for order: {}. Message will be requeued.", orderNo, e);
            channel.basicNack(deliveryTag, false, true);  // Requeue for retry
            throw new RuntimeException(e);
        }
    }

}


