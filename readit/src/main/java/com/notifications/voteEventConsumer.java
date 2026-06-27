package com.notifications;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class voteEventConsumer {

    @KafkaListener(
            topics = "${app.kafka.topics.vote-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ConsumerRecord<String, voteEvent> record, Acknowledgment ack) {

        voteEvent event = record.value();

        log.info("Received {} | targetId={} ({}) | partition={} | offset={}",
                event.getEventType(),
                record.key(),
                event.getTargetType(),
                record.partition(),
                record.offset());

        try {
            switch (event.getEventType()) {
                case UPVOTED       -> handleUpvote(event);
                case DOWNVOTED     -> handleDownvote(event);
                case VOTE_REMOVED  -> handleVoteRemoved(event);
                default -> log.warn("Unknown event type: {}", event.getEventType());
            }

            ack.acknowledge();

        } catch (Exception ex) {
            log.error("Error processing {} for targetId={}: {}",
                    event.getEventType(), event.getTargetId(), ex.getMessage(), ex);
        }
    }

    private void handleUpvote(voteEvent event) {
        log.info("[UPVOTED] targetId={} ({}) by userId={} | newScore={}",
                event.getTargetId(), event.getTargetType(),
                event.getUserId(), event.getScore());
        // TODO: update score in DB, update Redis feed ranking
    }

    private void handleDownvote(voteEvent event) {
        log.info("[DOWNVOTED] targetId={} ({}) by userId={} | newScore={}",
                event.getTargetId(), event.getTargetType(),
                event.getUserId(), event.getScore());
        // TODO: update score in DB, update Redis feed ranking
    }

    private void handleVoteRemoved(voteEvent event) {
        log.info("[VOTE REMOVED] targetId={} ({}) by userId={} | newScore={}",
                event.getTargetId(), event.getTargetType(),
                event.getUserId(), event.getScore());
        // TODO: update score in DB, update Redis feed ranking
    }
}