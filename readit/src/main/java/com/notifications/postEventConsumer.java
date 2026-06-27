package com.notifications;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class postEventConsumer {

    /**
     * Single listener covering all three event types on post.events.
     *
     * - groupId matches application.yml so it's consistent across restarts.
     * - Manual ack (MANUAL_IMMEDIATE) means the offset is only committed
     *   after we finish processing — gives at-least-once delivery.
     * - concurrency=3 (set in yml) spins up 3 threads, each owning 2 of
     *   the 6 partitions.
     */
    @KafkaListener(
            topics = "${app.kafka.topics.post-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, postEvent> record, Acknowledgment ack) {

        postEvent event = record.value();

        log.info("Received {} | postId={} | partition={} | offset={}",
                event.getEventType(),
                record.key(),
                record.partition(),
                record.offset());

        try {
            switch (event.getEventType()) {
                case POST_CREATED -> handleCreated(event);
                case POST_UPDATED -> handleUpdated(event);
                case POST_DELETED -> handleDeleted(event);
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }

            // Commit offset only after successful processing
            ack.acknowledge();

        } catch (Exception ex) {
            // Log and don't ack → message will be redelivered.
            // Wire in a DeadLetterPublishingRecoverer here for production use.
            log.error("Error processing {} for postId={}: {}",
                    event.getEventType(), event.getPostId(), ex.getMessage(), ex);
        }
    }

    // ── Handlers — replace stubs with real business logic ────────────────────

    private void handleCreated(postEvent event) {
        log.info("[POST CREATED] postId={} title='{}' author={}",
                event.getPostId(), event.getTitle(), event.getAuthorId());
        // e.g. persist to DB, update search index, send notification
    }

    private void handleUpdated(postEvent event) {
        log.info("[POST UPDATED] postId={} newTitle='{}'",
                event.getPostId(), event.getTitle());
        // e.g. update DB record, invalidate cache
    }

    private void handleDeleted(postEvent event) {
        log.info("[POST DELETED] postId={}", event.getPostId());
        // e.g. soft-delete in DB, remove from search index
    }
}