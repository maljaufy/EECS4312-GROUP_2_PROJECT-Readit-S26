package com.redditclone.voting.repository;

import com.redditclone.voting.domain.Vote;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByVoterIdAndTargetTypeAndTargetId(
            Long voterId,
            VoteTargetType targetType,
            Long targetId
    );

    long countByTargetTypeAndTargetIdAndValue(
            VoteTargetType targetType,
            Long targetId,
            VoteValue value
    );

    default int calculateScore(VoteTargetType targetType, Long targetId) {
        long upvotes = countByTargetTypeAndTargetIdAndValue(targetType, targetId, VoteValue.UPVOTE);
        long downvotes = countByTargetTypeAndTargetIdAndValue(targetType, targetId, VoteValue.DOWNVOTE);
        return Math.toIntExact(upvotes - downvotes);
    }
}
