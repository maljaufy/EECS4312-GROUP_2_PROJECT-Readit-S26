package com.redditclone.voting.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;

import java.time.LocalDateTime;

/** Stable Kafka wire representation of {@link VoteCastEvent}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VoteEventMessage(
        String eventId,
        LocalDateTime occurredAt,
        VoteTargetType targetType,
        Long targetId,
        Long userId,
        Long authorId,
        String username,
        VoteValue currentVote,
        int delta,
        int newScore
) {
}
