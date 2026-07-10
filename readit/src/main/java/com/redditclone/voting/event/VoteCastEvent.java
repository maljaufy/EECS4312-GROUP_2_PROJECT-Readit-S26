package com.redditclone.voting.event;
import com.redditclone.shared.event.DomainEvent;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;
import lombok.Getter;

@Getter
public class VoteCastEvent extends DomainEvent {

    private final VoteTargetType targetType;
    private final Long targetId;
    private final Long userId;
    private final String username;
    private final VoteValue currentVote;
    private final int delta;
    private final int newScore;

    /**
     * Backwards-compatible constructor for a post vote event.
     */
    public VoteCastEvent(Long postId, Long userId, String username, int delta, int newScore) {
        this(VoteTargetType.POST, postId, userId, username,
                delta >= 0 ? VoteValue.UPVOTE : VoteValue.DOWNVOTE, delta, newScore);
    }

    public VoteCastEvent(VoteTargetType targetType, Long targetId, Long userId,
                         String username, VoteValue currentVote, int delta, int newScore) {
        super();
        this.targetType = targetType;
        this.targetId = targetId;
        this.userId = userId;
        this.username = username;
        this.currentVote = currentVote;
        this.delta = delta;
        this.newScore = newScore;
    }

    /**
     * Retained for consumers of the original post-only event schema. Returns
     * {@code null} for comment votes; consumers should use targetType/targetId.
     */
    public Long getPostId() {
        return targetType == VoteTargetType.POST ? targetId : null;
    }

    @Override
    public String getAggregateId() {
        return targetType == VoteTargetType.POST
                ? String.valueOf(targetId)
                : targetType + ":" + targetId;
    }

    @Override
    public String getEventType() {
        return "VoteCastEvent";
    }
}
