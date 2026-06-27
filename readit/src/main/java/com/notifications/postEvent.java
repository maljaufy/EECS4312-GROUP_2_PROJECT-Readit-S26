package com.example.events.post.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Canonical event envelope written to / read from the post.events topic.
 *
 * Partition key  → postId  (so all events for a post land on the same partition,
 *                           preserving order per post)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostEvent {

    public enum EventType {
        POST_CREATED,
        POST_UPDATED,
        POST_DELETED
    }

    /** The type of action that triggered this event. */
    private EventType eventType;

    /** Unique identifier of the post. Used as the Kafka message key. */
    private String postId;

    /** ID of the user who performed the action. */
    private String authorId;

    /** Post title — present for CREATED / UPDATED, null for DELETED. */
    private String title;
    
    /** Post body content — present for CREATED / UPDATED, null for DELETED. */
    private String content;

    /** UTC timestamp when the event was produced. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;
}