package com.abel.ecommerce.config;

import com.rabbitmq.client.AMQP;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange("seckill.exchange");
    }

    @Bean
    public Queue seckillOrderQueue() {
        return new Queue("seckill.order.queue", true); // true = persistence
    }

    @Bean
    public Binding seckillBinding() {
        return BindingBuilder
                .bind(seckillOrderQueue())
                .to(seckillExchange())
                .with("seckill.order");
    }

    // Timeout components: for temporarily store messages
    @Bean
    public DirectExchange paymentTimeoutExchange() {
        return new DirectExchange("payment.timeout.exchange");
    }

    @Bean
    Queue paymentTimeoutQueue() {
        return QueueBuilder.durable("payment.timeout.queue")
                .ttl(15 * 60 * 1000)  // 15 minutes in milliseconds
                .deadLetterExchange("payment.check.exchange") // where to go when expired
                .deadLetterRoutingKey("payment.check")
                .build();
    }

    @Bean
    public Binding paymentTimeoutBinding() {
        return BindingBuilder
                .bind(paymentTimeoutQueue())
                .to(paymentTimeoutExchange())
                .with("payment.timeout");
    }


    // Dead letter components
    @Bean
    public DirectExchange paymentCheckExchange() {
        return new DirectExchange("payment.check.exchange");
    }

    @Bean
    public Queue paymentCheckQueue() {
        return new Queue("payment.check.queue", true);
    }

    @Bean
    public Binding paymentCheckBinding() {
        return BindingBuilder
                .bind(paymentCheckQueue())
                .to(paymentCheckExchange()) // Expired messages route here
                .with("payment.check");
    }

}
