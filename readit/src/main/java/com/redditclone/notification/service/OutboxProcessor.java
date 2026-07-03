package com.redditclone.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redditclone.notification.domain.OutboxEvent;
import com.redditclone.notification.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Scheduled task to publish pending events to Kafka.
     * Runs every 5 seconds.
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findTop100ByStatusPending();
        if (pendingEvents.isEmpty()) {
            return;
        }

        log.info("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = getTopicForEventType(event.getEventType());
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload());
                event.setStatus("PUBLISHED");
                event.setPublishedAt(LocalDateTime.now());
                outboxRepository.save(event);
                log.debug("Published outbox event: {} to {}", event.getEventType(), topic);
            } catch (Exception e) {
                log.error("Failed to publish outbox event: {}", event.getId(), e);
                event.setStatus("FAILED");
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
                outboxRepository.save(event);
            }
        }
    }

    /**
     * Maps event types to Kafka topic names.
     */
    private String getTopicForEventType(String eventType) {
        return switch (eventType) {
            case "UserRegisteredEvent" -> "user.events";
            case "PostCreatedEvent" -> "post.events";
            case "VoteCastEvent" -> "vote.events";
            case "CommentAddedEvent" -> "comment.events";
            case "NotificationEvent" -> "notification.events";
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }

}
