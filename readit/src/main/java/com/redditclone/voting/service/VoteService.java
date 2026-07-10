package com.redditclone.voting.service;

import com.redditclone.posts.domain.Post;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.shared.event.EventPublisher;
import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import com.redditclone.voting.domain.Vote;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.event.VoteCastEvent;
import com.redditclone.voting.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class VoteService {
    @Autowired
    private EventPublisher eventPublisher;

    private final VoteRepository voteRepository;
    private final PostRepository postRepository;
    private final UserService userService;

    public VoteService(VoteRepository voteRepository, PostRepository postRepository, UserService userService) {
        this.voteRepository = voteRepository;
        this.postRepository = postRepository;
        this.userService = userService;
    }

    @Transactional
    public VoteResult upvotePost(Long voterId, Long postId) {
        return voteOnPost(voterId, postId, VoteValue.UPVOTE);
    }

    @Transactional
    public VoteResult downvotePost(Long voterId, Long postId) {
        return voteOnPost(voterId, postId, VoteValue.DOWNVOTE);
    }

    @Transactional
    public VoteResult voteOnPost(Long voterId, Long postId, VoteValue value) {
        Post post = requirePostVoteInput(voterId, postId, value);
        Long postAuthorId = post.getAuthor().getId();

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
    public VoteResult removePostVote(Long voterId, Long postId) {
        Post post = requirePostVoteInput(voterId, postId, VoteValue.UPVOTE);
        Long postAuthorId = post.getAuthor().getId();

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

    private Post requirePostVoteInput(Long voterId, Long postId, VoteValue value) {
        Objects.requireNonNull(voterId, "voterId must not be null");
        Objects.requireNonNull(postId, "postId must not be null");
        Objects.requireNonNull(value, "value must not be null");

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));
        Long postAuthorId = post.getAuthor().getId();

        if (voterId.equals(postAuthorId)) {
            throw new IllegalArgumentException("Users cannot vote on their own posts");
        }

        userService.findById(voterId);
        return post;
    }

    @Transactional
    public void castVote(Long userId, Long postId, int delta) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));

        VoteValue voteValue = delta > 0 ? VoteValue.UPVOTE : VoteValue.DOWNVOTE;
        Vote existingVote = voteRepository.findByVoterIdAndTargetTypeAndTargetId(
                userId,
                VoteTargetType.POST,
                postId
        ).orElse(null);

        Vote vote;
        if (existingVote == null) {
            vote = new Vote(userId, VoteTargetType.POST, postId, voteValue);
        } else {
            existingVote.setValue(voteValue);
            vote = existingVote;
        }

        Vote saved = voteRepository.save(vote);

        int newScore = voteRepository.calculateScore(VoteTargetType.POST, postId);

        User voter = userService.findById(userId);

        eventPublisher.publish(new VoteCastEvent(
                postId,
                userId,
                voter.getUsername(),
                delta,
                newScore
        ));
    }
}
