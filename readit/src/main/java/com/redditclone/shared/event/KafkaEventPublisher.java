package com.redditclone.shared.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /*
    Kafka implementation
    */

    @Override
    public void publish(DomainEvent event) {
        try {
            String topic = event.getEventType().toLowerCase();
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, event.getAggregateId(), payload);
            log.info("Published event {} to topic {}", event.getEventType(), topic);
        } catch (Exception e) {
            log.error("Failed to publish event {}", event.getEventType(), e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}
