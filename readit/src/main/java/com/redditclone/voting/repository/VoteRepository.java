package com.redditclone.voting.repository;

import com.redditclone.voting.domain.Vote;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Lock(LockModeType.OPTIMISTIC)
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

    void deleteByTargetTypeAndTargetId(VoteTargetType targetType, Long targetId);

    default int calculateScore(VoteTargetType targetType, Long targetId) {
        long upvotes = countByTargetTypeAndTargetIdAndValue(targetType, targetId, VoteValue.UPVOTE);
        long downvotes = countByTargetTypeAndTargetIdAndValue(targetType, targetId, VoteValue.DOWNVOTE);
        return Math.toIntExact(upvotes - downvotes);
    }
}
