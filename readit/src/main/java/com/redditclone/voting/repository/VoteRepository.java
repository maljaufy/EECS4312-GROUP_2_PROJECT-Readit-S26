package com.redditclone.voting.repository;

import com.redditclone.voting.domain.Vote;
import com.redditclone.voting.domain.VoteTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByVoterIdAndTargetTypeAndTargetId(
            Long voterId,
            VoteTargetType targetType,
            Long targetId
    );
}
