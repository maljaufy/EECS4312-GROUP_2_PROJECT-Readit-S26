package com.notifications;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class postEvent {

    public enum EventType {
        POST_CREATED,
        POST_UPDATED,
        POST_DELETED
    }

    private EventType eventType;
    private String postId;
    private String authorId;   // was userId
    private String title;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;  // was createdAt
}