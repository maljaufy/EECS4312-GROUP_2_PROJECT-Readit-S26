package com.notifications;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class commentEventConsumer {

    @KafkaListener(
            topics = "${app.kafka.topics.comment-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, commentEvent> record, Acknowledgment ack) {

        commentEvent event = record.value();

        log.info("Received {} | commentId={} | partition={} | offset={}",
                event.getEventType(),
                record.key(),
                record.partition(),
                record.offset());

        try {
            switch (event.getEventType()) {
                case COMMENT_CREATED -> handleCreated(event);
                case COMMENT_UPDATED -> handleUpdated(event);
                case COMMENT_DELETED -> handleDeleted(event);
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }

            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Error processing {} for commentId={}: {}",
                    event.getEventType(), event.getCommentId(), ex.getMessage(), ex);
        }
    }

    private void handleCreated(commentEvent event) {
        log.info("[COMMENT CREATED] commentId={} on postId={} by authorId={} | reply={}",
                event.getCommentId(), event.getPostId(), event.getAuthorId(),
                event.getParentCommentId() != null ? "yes, under " + event.getParentCommentId() : "no");
        // TODO: persist to DB, notify post author
    }

    private void handleUpdated(commentEvent event) {
        log.info("[COMMENT UPDATED] commentId={} on postId={}",
                event.getCommentId(), event.getPostId());
        // TODO: update DB record, invalidate cache
    }

    private void handleDeleted(commentEvent event) {
        log.info("[COMMENT DELETED] commentId={} on postId={}",
                event.getCommentId(), event.getPostId());
        // TODO: soft-delete in DB
    }
}