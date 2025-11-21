package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.SeckillMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeckillMessageRepository extends JpaRepository<SeckillMessage, Long> {

    /**
     * Find message by order number
     */
    Optional<SeckillMessage> findByOrderNo(String orderNo);

    /**
     * Find all pending messages that need retry (status = 0, retry_count < max_retry, next_retry_time <= now)
     */
    @Query("SELECT sm FROM SeckillMessage sm WHERE sm.status = 0 AND sm.retryCount < sm.maxRetry " +
           "AND (sm.nextRetryTime IS NULL OR sm.nextRetryTime <= :now)")
    List<SeckillMessage> findPendingMessagesForRetry(@Param("now") LocalDateTime now);

    /**
     * Find all failed messages (status = 2 or retry_count >= max_retry)
     */
    @Query("SELECT sm FROM SeckillMessage sm WHERE sm.status = 2 OR sm.retryCount >= sm.maxRetry")
    List<SeckillMessage> findFailedMessages();

    /**
     * Update message status
     */
    @Modifying
    @Query("UPDATE SeckillMessage sm SET sm.status = :status WHERE sm.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /**
     * Increment retry count and set next retry time
     */
    @Modifying
    @Query("UPDATE SeckillMessage sm SET sm.retryCount = sm.retryCount + 1, sm.nextRetryTime = :nextRetryTime " +
           "WHERE sm.id = :id")
    void incrementRetryCount(@Param("id") Long id, @Param("nextRetryTime") LocalDateTime nextRetryTime);

    /**
     * Find messages by user ID
     */
    List<SeckillMessage> findByUserId(Long userId);

    /**
     * Find messages by product ID
     */
    List<SeckillMessage> findByProductId(Long productId);

    /**
     * Find messages by status
     */
    List<SeckillMessage> findByStatus(Integer status);

    /**
     * Delete old messages (cleanup job) Logical deletion
     */
    @Modifying
    @Query("DELETE FROM SeckillMessage sm WHERE sm.status = 1 AND sm.createdAt < :before")
    void deleteOldSuccessfulMessages(@Param("before") LocalDateTime before);

    /**
     * Count messages by status
     */
    long countByStatus(Integer status);
}
