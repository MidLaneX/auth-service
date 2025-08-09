package com.midlane.project_management_tool_auth_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(String topic, String message) {
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Message sent successfully to topic [{}] with offset=[{}]: {}",
                            topic, result.getRecordMetadata().offset(), message);
                } else {
                    logger.error("Failed to send message to topic [{}]: {}", topic, message, ex);
                }
            });
        } catch (Exception e) {
            logger.error("Error sending message to Kafka topic [{}]: {}", topic, message, e);
        }
    }

    public void sendMessageWithKey(String topic, String key, String message) {
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, message);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Message sent successfully to topic [{}] with key [{}] and offset=[{}]: {}",
                            topic, key, result.getRecordMetadata().offset(), message);
                } else {
                    logger.error("Failed to send message to topic [{}] with key [{}]: {}", topic, key, message, ex);
                }
            });
        } catch (Exception e) {
            logger.error("Error sending message with key to Kafka topic [{}]: {}", topic, message, e);
        }
    }

    // Synchronous send for critical messages where you need to ensure delivery
    public void sendMessageSync(String topic, String message) {
        try {
            SendResult<String, String> result = kafkaTemplate.send(topic, message).get();
            logger.info("Message sent synchronously to topic [{}] with offset=[{}]: {}",
                    topic, result.getRecordMetadata().offset(), message);
        } catch (Exception e) {
            logger.error("Error sending message synchronously to Kafka topic [{}]: {}", topic, message, e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }
}
