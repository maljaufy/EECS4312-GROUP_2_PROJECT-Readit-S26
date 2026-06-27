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
public class voteEvent {

    public enum EventType {
        UPVOTED,
        DOWNVOTED,
        VOTE_REMOVED
    }

    public enum TargetType {
        POST,
        COMMENT
    }

    private EventType eventType;

    /** What was voted on — a post or a comment */
    private TargetType targetType;

    /** ID of the post or comment being voted on. Used as partition key. */
    private String targetId;

    /** ID of the user casting the vote */
    private String userId;

    /** Running score after this vote was applied */
    private int score;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;
}