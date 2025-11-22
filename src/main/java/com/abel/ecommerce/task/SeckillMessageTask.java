package com.abel.ecommerce.task;

import com.abel.ecommerce.entity.SeckillMessage;
import com.abel.ecommerce.repository.SeckillMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillMessageTask {

    private final SeckillMessageRepository messageRepository;

    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedRate = 5000)
    public void scanAndSend() {
        List<SeckillMessage> pendingMessages = messageRepository.findPendingMessagesForRetry(LocalDateTime.now());

        if (pendingMessages.isEmpty()) {
            return;
        }

        log.info("Found {} pending messages to retry", pendingMessages.size());

        for (SeckillMessage message : pendingMessages) {
            try {
                rabbitTemplate.convertAndSend(message.getExchange(), message.getRoutingKey(), message.getMessageContent());
                message.setStatus(SeckillMessage.STATUS_SENT);

                log.info("Successfully sent message id={} to exchange={}, routingKey={}",
                        message.getId(), message.getExchange(), message.getRoutingKey());
            }
            catch (Exception e) {
                message.setRetryCount(message.getRetryCount() + 1);
                message.setNextRetryTime(LocalDateTime.now().plusSeconds(30));

                if (message.getRetryCount() >= message.getMaxRetry()) {
                    message.setStatus(SeckillMessage.STATUS_FAILED);
                    log.error("Message id={} failed after {} retries. Marking as FAILED",
                            message.getId(), message.getRetryCount(), e);
                }
                else {
                    log.warn("Failed to send message id={}, retry count={}/{}",
                            message.getId(), message.getRetryCount(), message.getMaxRetry(), e);
                }
            }
            messageRepository.save(message);
        }
    }

}
