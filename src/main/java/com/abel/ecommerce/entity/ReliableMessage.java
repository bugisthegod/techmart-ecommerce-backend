package com.abel.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "reliable_message")
public class ReliableMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, length = 100)
    private String messageId;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "consume_time", nullable = false, updatable = false)
    private LocalDateTime consumeTime;

    @PrePersist
    protected void onCreate() {
        if (consumeTime == null) {
            consumeTime = LocalDateTime.now();
        }
    }
}
