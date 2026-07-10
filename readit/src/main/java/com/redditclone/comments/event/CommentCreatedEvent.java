package com.redditclone.comments.event;

import com.redditclone.shared.event.DomainEvent;
import lombok.Getter;

@Getter
public class CommentCreatedEvent extends DomainEvent{

    private final Long commentId;
    private final Long postId;
    private final String authorUsername;
    private final String text;
    private final Long parentCommentId;

    public CommentCreatedEvent(Long commentId, Long postId, String authorUsername, String text, Long parentCommentId) {
        super();
        this.commentId = commentId;
        this.postId = postId;
        this.authorUsername = authorUsername;
        this.text = text;
        this.parentCommentId = parentCommentId;
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(commentId);
    }

    @Override
    public String getEventType() {
        return "CommentCreatedEvent";
    }
}
