package com.notifications;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Canonical event envelope written to / read from the post.events topic.
 *
 * Partition key  → commentID  (so all events for a comment land on the same partition,
 *                           preserving order per comment)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class commentEvent {

    public enum EventType {
        COMMENT_CREATED,
        COMMENT_UPDATED,
        COMMENT_DELETED
    }

    /** The type of action that triggered this event. */
    private EventType eventType;

    private String commentId;        // was commentID
    private String postId;           // add this
    private String authorId;         // was userID
    private String parentCommentId;  // was replyID
    private String content;

    /** UTC timestamp when the event was produced. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;      // was createdAt
}