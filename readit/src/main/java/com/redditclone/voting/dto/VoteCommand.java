package com.redditclone.voting.dto;

import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;
import jakarta.validation.constraints.NotNull;

public record VoteCommand(
        @NotNull Long voterId,
        @NotNull VoteTargetType targetType,
        @NotNull Long targetId,
        @NotNull VoteValue value
) {
}
