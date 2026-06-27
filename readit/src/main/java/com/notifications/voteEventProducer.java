package com.notifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class voteEventProducer {

    private final KafkaTemplate<String, voteEvent> kafkaTemplate;

    @Value("${app.kafka.topics.vote-events}")
    private String topic;

    public CompletableFuture<SendResult<String, voteEvent>> publishUpvote(
            String targetId, voteEvent.TargetType targetType, String userId, int score) {

        return send(voteEvent.builder()
                .eventType(voteEvent.EventType.UPVOTED)
                .targetId(targetId)
                .targetType(targetType)
                .userId(userId)
                .score(score)
                .occurredAt(Instant.now())
                .build());
    }

    public CompletableFuture<SendResult<String, voteEvent>> publishDownvote(
            String targetId, voteEvent.TargetType targetType, String userId, int score) {

        return send(voteEvent.builder()
                .eventType(voteEvent.EventType.DOWNVOTED)
                .targetId(targetId)
                .targetType(targetType)
                .userId(userId)
                .score(score)
                .occurredAt(Instant.now())
                .build());
    }

    public CompletableFuture<SendResult<String, voteEvent>> publishVoteRemoved(
            String targetId, voteEvent.TargetType targetType, String userId, int score) {

        return send(voteEvent.builder()
                .eventType(voteEvent.EventType.VOTE_REMOVED)
                .targetId(targetId)
                .targetType(targetType)
                .userId(userId)
                .score(score)
                .occurredAt(Instant.now())
                .build());
    }

    private CompletableFuture<SendResult<String, voteEvent>> send(voteEvent event) {
        log.info("Publishing {} for targetId={} ({})",
                event.getEventType(), event.getTargetId(), event.getTargetType());

        CompletableFuture<SendResult<String, voteEvent>> future =
                kafkaTemplate.send(topic, event.getTargetId(), event);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} for targetId={}: {}",
                        event.getEventType(), event.getTargetId(), ex.getMessage());
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