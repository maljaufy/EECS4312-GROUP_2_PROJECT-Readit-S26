package com.example.events.post.producer;

import com.example.events.post.model.PostEvent;
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
public class PostEventProducer {

    private final KafkaTemplate<String, PostEvent> kafkaTemplate;

    @Value("${app.kafka.topics.post-events}")
    private String topic;

    // ── Public API ────────────────────────────────────────────────────────────

    public CompletableFuture<SendResult<String, PostEvent>> publishCreated(
            String authorId, String title, String content) {

        PostEvent event = PostEvent.builder()
                .eventType(PostEvent.EventType.POST_CREATED)
                .postId(UUID.randomUUID().toString())
                .authorId(authorId)
                .title(title)
                .content(content)
                .occurredAt(Instant.now())
                .build();

        return send(event);
    }

    public CompletableFuture<SendResult<String, PostEvent>> publishUpdated(
            String postId, String title, String content) {

        PostEvent event = PostEvent.builder()
                .eventType(PostEvent.EventType.POST_UPDATED)
                .postId(postId)
                .title(title)
                .content(content)
                .occurredAt(Instant.now())
                .build();

        return send(event);
    }

    public CompletableFuture<SendResult<String, PostEvent>> publishDeleted(String postId) {

        PostEvent event = PostEvent.builder()
                .eventType(PostEvent.EventType.POST_DELETED)
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
    private CompletableFuture<SendResult<String, PostEvent>> send(PostEvent event) {
        log.info("Publishing {} for postId={}", event.getEventType(), event.getPostId());

        CompletableFuture<SendResult<String, PostEvent>> future =
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