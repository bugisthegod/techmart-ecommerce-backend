package com.abel.ecommerce.repository;

import com.abel.ecommerce.entity.ReliableMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReliableMessageRepository extends JpaRepository<ReliableMessage, Long> {

    /**
     * Find message by message ID
     */
    Optional<ReliableMessage> findByMessageId(String messageId);

    /**
     * Check if message exists by message ID (for deduplication)
     */
    boolean existsByMessageId(String messageId);

    /**
     * Find all messages by consumer name
     */
    List<ReliableMessage> findByConsumerName(String consumerName);

    /**
     * Find messages consumed within a time range
     */
    @Query("SELECT rm FROM ReliableMessage rm WHERE rm.consumeTime BETWEEN :startTime AND :endTime")
    List<ReliableMessage> findByConsumeTimeBetween(@Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Find messages by consumer name and time range
     */
    @Query("SELECT rm FROM ReliableMessage rm WHERE rm.consumerName = :consumerName " +
           "AND rm.consumeTime BETWEEN :startTime AND :endTime")
    List<ReliableMessage> findByConsumerNameAndConsumeTimeBetween(
            @Param("consumerName") String consumerName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Delete old consumption records (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM ReliableMessage rm WHERE rm.consumeTime < :before")
    void deleteOldRecords(@Param("before") LocalDateTime before);

    /**
     * Count messages by consumer name
     */
    long countByConsumerName(String consumerName);

    /**
     * Count messages consumed after a specific time
     */
    @Query("SELECT COUNT(rm) FROM ReliableMessage rm WHERE rm.consumeTime >= :afterTime")
    long countMessagesConsumedAfter(@Param("afterTime") LocalDateTime afterTime);
}