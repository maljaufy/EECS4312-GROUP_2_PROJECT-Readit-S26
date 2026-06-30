package com.redditclone.voting.service;

import com.redditclone.user.service.UserService;
import com.redditclone.voting.domain.Vote;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final UserService userService;

    public VoteService(VoteRepository voteRepository, UserService userService) {
        this.voteRepository = voteRepository;
        this.userService = userService;
    }

    @Transactional
    public VoteResult upvotePost(Long voterId, Long postId, Long postAuthorId) {
        return voteOnPost(voterId, postId, postAuthorId, VoteValue.UPVOTE);
    }

    @Transactional
    public VoteResult downvotePost(Long voterId, Long postId, Long postAuthorId) {
        return voteOnPost(voterId, postId, postAuthorId, VoteValue.DOWNVOTE);
    }

    @Transactional
    public VoteResult voteOnPost(Long voterId, Long postId, Long postAuthorId, VoteValue value) {
        requirePostVoteInput(voterId, postId, postAuthorId, value);

        Vote existingVote = voteRepository.findByVoterIdAndTargetTypeAndTargetId(
                voterId,
                VoteTargetType.POST,
                postId
        ).orElse(null);

        if (existingVote == null) {
            voteRepository.save(new Vote(voterId, VoteTargetType.POST, postId, value));
            userService.updateKarma(postAuthorId, value.getKarmaDelta());
            return result(postId, value, value.getKarmaDelta(), true);
        }

        if (existingVote.getValue() == value) {
            return result(postId, value, 0, false);
        }

        int karmaDelta = value.getKarmaDelta() - existingVote.getValue().getKarmaDelta();
        existingVote.setValue(value);
        voteRepository.save(existingVote);
        userService.updateKarma(postAuthorId, karmaDelta);
        return result(postId, value, karmaDelta, true);
    }

    @Transactional
    public VoteResult removePostVote(Long voterId, Long postId, Long postAuthorId) {
        requirePostVoteInput(voterId, postId, postAuthorId, VoteValue.UPVOTE);

        return voteRepository.findByVoterIdAndTargetTypeAndTargetId(voterId, VoteTargetType.POST, postId)
                .map(existingVote -> {
                    int karmaDelta = -existingVote.getValue().getKarmaDelta();
                    voteRepository.delete(existingVote);
                    userService.updateKarma(postAuthorId, karmaDelta);
                    return result(postId, null, karmaDelta, true);
                })
                .orElseGet(() -> result(postId, null, 0, false));
    }

    @Transactional(readOnly = true)
    public int getPostScore(Long postId) {
        Objects.requireNonNull(postId, "postId must not be null");
        return voteRepository.calculateScore(VoteTargetType.POST, postId);
    }

    private VoteResult result(Long postId, VoteValue currentVote, int karmaDelta, boolean changed) {
        return new VoteResult(
                VoteTargetType.POST,
                postId,
                currentVote,
                voteRepository.calculateScore(VoteTargetType.POST, postId),
                karmaDelta,
                changed
        );
    }

    private void requirePostVoteInput(Long voterId, Long postId, Long postAuthorId, VoteValue value) {
        Objects.requireNonNull(voterId, "voterId must not be null");
        Objects.requireNonNull(postId, "postId must not be null");
        Objects.requireNonNull(postAuthorId, "postAuthorId must not be null");
        Objects.requireNonNull(value, "value must not be null");

        if (voterId.equals(postAuthorId)) {
            throw new IllegalArgumentException("Users cannot vote on their own posts");
        }

        // TODO: Replace postId/postAuthorId parameters with a Post lookup once the Post entity/service lands.
    }
}
