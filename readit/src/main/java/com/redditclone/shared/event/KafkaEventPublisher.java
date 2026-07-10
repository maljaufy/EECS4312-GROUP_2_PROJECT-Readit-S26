package com.redditclone.shared.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redditclone.notification.domain.OutboxEvent;
import com.redditclone.notification.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setEventType(event.getEventType());
            outboxEvent.setAggregateId(event.getAggregateId());
            outboxEvent.setPayload(payload);
            outboxEvent.setStatus("PENDING");
            outboxRepository.save(outboxEvent);
            log.debug("Stored event in outbox: {} ({})", event.getEventType(), event.getEventId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event: {}", event.getEventType(), e);
            throw new RuntimeException("Failed to serialize event", e);
        }
    }
}
