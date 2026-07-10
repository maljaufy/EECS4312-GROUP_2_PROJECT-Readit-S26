package com.redditclone.voting.event;
import com.redditclone.shared.event.DomainEvent;
import lombok.Getter;

@Getter
public class VoteCastEvent extends DomainEvent{

    private final Long postId;
    private final Long userId;
    private final String username;
    private final int delta; // +1 for upvote, -1 for downvote
    private final int newScore;

    public VoteCastEvent(Long postId, Long userId, String username, int delta, int newScore) {
        super();
        this.postId = postId;
        this.userId = userId;
        this.username = username;
        this.delta = delta;
        this.newScore = newScore;
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(postId);
    }

    @Override
    public String getEventType() {
        return "VoteCastEvent";
    }
}
