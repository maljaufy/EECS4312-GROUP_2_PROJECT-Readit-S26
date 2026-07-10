package com.redditclone.posts.event;

import com.redditclone.shared.event.DomainEvent;
import lombok.Getter;

@Getter
public class PostCreatedEvent extends DomainEvent {
    private final Long postId;
    private final String title;
    private final String authorUsername;
    private final String subredditName;

    public PostCreatedEvent(Long postId, String title, String authorUsername, String subredditName) {
        super();
        this.postId = postId;
        this.title = title;
        this.authorUsername = authorUsername;
        this.subredditName = subredditName;
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(postId);
    }

    @Override
    public String getEventType() {
        return "PostCreatedEvent";
    }
}
