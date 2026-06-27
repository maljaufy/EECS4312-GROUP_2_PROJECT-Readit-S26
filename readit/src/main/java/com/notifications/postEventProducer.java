package com.notifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class postEventProducer {

    private final KafkaTemplate<String, postEvent> kafkaTemplate;

    @Value("${app.kafka.topics.post-events}")
    private String topic;


    public CompletableFuture<SendResult<String, postEvent>> publishCreated(
            String authorId, String title, String content) {

        postEvent event = postEvent.builder()
                .eventType(postEvent.EventType.POST_CREATED)
                .postId(UUID.randomUUID().toString())
                .authorId(authorId)
                .title(title)
                .content(content)
                .occurredAt(Instant.now())
                .build();

        return send(event);
    }

    public CompletableFuture<SendResult<String, postEvent>> publishUpdated(
            String postId, String title, String content) {

        postEvent event = postEvent.builder()
                .eventType(postEvent.EventType.POST_UPDATED)
                .postId(postId)
                .title(title)
                .content(content)
                .occurredAt(Instant.now())
                .build();

        return send(event);
    }

    public CompletableFuture<SendResult<String, postEvent>> publishDeleted(String postId) {

        postEvent event = postEvent.builder()
                .eventType(postEvent.EventType.POST_DELETED)
                .postId(postId)
                .occurredAt(Instant.now())
                .build();

        return send(event);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    /**
     * Sends an event using the postId as the partition key so all events
     * for the same post land on the same partition (ordering guarantee).
     */
    private CompletableFuture<SendResult<String, postEvent>> send(postEvent event) {
        log.info("Publishing {} for postId={}", event.getEventType(), event.getPostId());

        CompletableFuture<SendResult<String, postEvent>> future =
                kafkaTemplate.send(topic, event.getPostId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} for postId={}: {}",
                        event.getEventType(), event.getPostId(), ex.getMessage());
            } else {
                log.debug("Published {} → partition={} offset={}",
                        event.getEventType(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });

        return future;
    }
}