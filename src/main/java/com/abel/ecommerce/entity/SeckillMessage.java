package com.abel.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "seckill_message")
@Data
public class SeckillMessage {

    // Status constants
    public static final Integer STATUS_PENDING = 0;
    public static final Integer STATUS_SENT = 1;
    public static final Integer STAT1US_FAILED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, length = 100)
    private String exchange;

    @Column(name = "routing_key", nullable = false, length = 100)
    private String routingKey;

    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    @Column(columnDefinition = "TINYINT DEFAULT 0")
    private Integer status = STATUS_PENDING;

    @Column(name = "retry_count", columnDefinition = "INT DEFAULT 0")
    private Integer retryCount = 0;

    @Column(name = "max_retry", columnDefinition = "INT DEFAULT 3")
    private Integer maxRetry = 3;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;

    @Version
    @Column(columnDefinition = "INT DEFAULT 1")
    private Integer version = 1;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = STATUS_PENDING;
        if (retryCount == null) retryCount = 0;
        if (maxRetry == null) maxRetry = 3;
        if (version == null) version = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
