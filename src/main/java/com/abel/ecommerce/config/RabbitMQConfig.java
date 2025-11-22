package com.abel.ecommerce.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
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

}
