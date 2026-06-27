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
public class commentEventProducer {

    private final KafkaTemplate<String, commentEvent> kafkaTemplate;

    @Value("${app.kafka.topics.comment-events}")
    private String topic;

    public CompletableFuture<SendResult<String, commentEvent>> publishCreated(
            String postId, String authorId, String content, String parentCommentId) {

        commentEvent event = commentEvent.builder()
                .eventType(commentEvent.EventType.COMMENT_CREATED)
                .commentId(UUID.randomUUID().toString())
                .postId(postId)
                .parentCommentId(parentCommentId) // null if top-level comment
                .authorId(authorId)
                .content(content)
                .occurredAt(Instant.now())
                .build();

        return send(event);
    }

    public CompletableFuture<SendResult<String, commentEvent>> publishUpdated(
            String commentId, String postId, String content) {

        commentEvent event = commentEvent.builder()
                .eventType(commentEvent.EventType.COMMENT_UPDATED)
                .commentId(commentId)
                .postId(postId)
                .content(content)
                .occurredAt(Instant.now())
                .build();

        return send(event);
    }

    public CompletableFuture<SendResult<String, commentEvent>> publishDeleted(
            String commentId, String postId) {

        commentEvent event = commentEvent.builder()
                .eventType(commentEvent.EventType.COMMENT_DELETED)
                .commentId(commentId)
                .postId(postId)
                .occurredAt(Instant.now())
                .build();

        return send(event);
    }

    private CompletableFuture<SendResult<String, commentEvent>> send(commentEvent event) {
        log.info("Publishing {} for commentId={}", event.getEventType(), event.getCommentId());

        CompletableFuture<SendResult<String, commentEvent>> future =
                kafkaTemplate.send(topic, event.getCommentId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} for commentId={}: {}",
                        event.getEventType(), event.getCommentId(), ex.getMessage());
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