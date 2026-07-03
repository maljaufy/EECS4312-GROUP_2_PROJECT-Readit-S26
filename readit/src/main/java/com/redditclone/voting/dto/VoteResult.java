package com.redditclone.voting.dto;

import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;

public record VoteResult(
        VoteTargetType targetType,
        Long targetId,
        VoteValue currentVote,
        int score,
        int karmaDelta,
        boolean changed
) {
}
