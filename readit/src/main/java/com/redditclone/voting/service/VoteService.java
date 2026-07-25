package com.redditclone.voting.service;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.shared.event.EventPublisher;
import com.redditclone.shared.push.UIBroadcaster;
import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import com.redditclone.voting.domain.Vote;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.event.VoteCastEvent;
import com.redditclone.voting.repository.VoteRepository;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;
import java.util.Optional;

@Service
public class VoteService {
    private final VoteRepository voteRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;
    private final EventPublisher eventPublisher;
    private final UIBroadcaster uiBroadcaster;

    @Autowired
    public VoteService(VoteRepository voteRepository, PostRepository postRepository,
                       CommentRepository commentRepository, UserService userService,
                       EventPublisher eventPublisher, UIBroadcaster uiBroadcaster) {
        this.voteRepository = voteRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
        this.uiBroadcaster = uiBroadcaster;
    }

    /** Kept for existing post-voting unit tests and callers. */
    public VoteService(VoteRepository voteRepository, PostRepository postRepository, UserService userService) {
        this(voteRepository, postRepository, null, userService, null, null);
    }

    @Transactional
    @Retry(name = "votePersistence")
    public VoteResult upvotePost(Long voterId, Long postId) {
        return voteOnPost(voterId, postId, VoteValue.UPVOTE);
    }

    @Transactional
    @Retry(name = "votePersistence")
    public VoteResult downvotePost(Long voterId, Long postId) {
        return voteOnPost(voterId, postId, VoteValue.DOWNVOTE);
    }

    @Transactional
    @Retry(name = "votePersistence")
    public VoteResult voteOnPost(Long voterId, Long postId, VoteValue value) {
        Post post = requirePostVoteInput(voterId, postId, value);
        return vote(voterId, VoteTargetType.POST, postId, post.getAuthor().getId(), value,
                score -> post.setVoteScore(score));
    }

    @Transactional
    @Retry(name = "votePersistence")
    public VoteResult removePostVote(Long voterId, Long postId) {
        Post post = requirePostVoteInput(voterId, postId, VoteValue.UPVOTE);
        return removeVote(voterId, VoteTargetType.POST, postId, post.getAuthor().getId(),
                score -> post.setVoteScore(score));
    }

    /**
     * Applies Reddit-style button behavior: selecting the active direction
     * removes the vote; selecting the other direction switches it.
     */
    @Transactional
    @Retry(name = "votePersistence")
    public VoteResult togglePostVote(Long voterId, Long postId, VoteValue value) {
        Objects.requireNonNull(voterId, "voterId must not be null");
        Objects.requireNonNull(postId, "postId must not be null");
        Objects.requireNonNull(value, "value must not be null");

        Optional<VoteValue> currentVote = getPostVote(voterId, postId);
        return currentVote.filter(value::equals).isPresent()
                ? removePostVote(voterId, postId)
                : voteOnPost(voterId, postId, value);
    }

    @Transactional(readOnly = true)
    public int getPostScore(Long postId) {
        return getScore(VoteTargetType.POST, postId);
    }

    @Transactional(readOnly = true)
    public Optional<VoteValue> getPostVote(Long voterId, Long postId) {
        if (voterId == null || postId == null) {
            return Optional.empty();
        }
        return voteRepository.findByVoterIdAndTargetTypeAndTargetId(
                        voterId, VoteTargetType.POST, postId)
                .map(Vote::getValue);
    }

    @Transactional
    @Retry(name = "votePersistence")
    public VoteResult upvoteComment(Long voterId, Long commentId) {
        return voteOnComment(voterId, commentId, VoteValue.UPVOTE);
    }

    @Transactional
    @Retry(name = "votePersistence")
    public VoteResult downvoteComment(Long voterId, Long commentId) {
        return voteOnComment(voterId, commentId, VoteValue.DOWNVOTE);
    }

    @Transactional
    @Retry(name = "votePersistence")
    public VoteResult voteOnComment(Long voterId, Long commentId, VoteValue value) {
        Comment comment = requireCommentVoteInput(voterId, commentId, value);
        return vote(voterId, VoteTargetType.COMMENT, commentId, comment.getAuthor().getId(), value,
                score -> comment.setVoteScore(score));
    }

    @Transactional
    @Retry(name = "votePersistence")
    public VoteResult removeCommentVote(Long voterId, Long commentId) {
        Comment comment = requireCommentVoteInput(voterId, commentId, VoteValue.UPVOTE);
        return removeVote(voterId, VoteTargetType.COMMENT, commentId, comment.getAuthor().getId(),
                score -> comment.setVoteScore(score));
    }

    /**
     * Applies the same Reddit-style toggle behavior as {@link #togglePostVote}:
     * selecting the currently-active direction removes the vote; selecting the
     * other direction switches it.
     */
    @Transactional
    @Retry(name = "votePersistence")
    public VoteResult toggleCommentVote(Long voterId, Long commentId, VoteValue value) {
        Objects.requireNonNull(voterId, "voterId must not be null");
        Objects.requireNonNull(commentId, "commentId must not be null");
        Objects.requireNonNull(value, "value must not be null");

        Optional<VoteValue> currentVote = getCommentVote(voterId, commentId);
        return currentVote.filter(value::equals).isPresent()
                ? removeCommentVote(voterId, commentId)
                : voteOnComment(voterId, commentId, value);
    }

    @Transactional(readOnly = true)
    public int getCommentScore(Long commentId) {
        return getScore(VoteTargetType.COMMENT, commentId);
    }

    @Transactional(readOnly = true)
    public Optional<VoteValue> getCommentVote(Long voterId, Long commentId) {
        if (voterId == null || commentId == null) {
            return Optional.empty();
        }
        return voteRepository.findByVoterIdAndTargetTypeAndTargetId(
                        voterId, VoteTargetType.COMMENT, commentId)
                .map(Vote::getValue);
    }

    private VoteResult vote(Long voterId, VoteTargetType targetType, Long targetId, Long authorId,
                            VoteValue value, ScoreUpdater scoreUpdater) {
        User voter = userService.findById(voterId);
        Vote existingVote = voteRepository.findByVoterIdAndTargetTypeAndTargetId(voterId, targetType, targetId)
                .orElse(null);

        if (existingVote != null && existingVote.getValue() == value) {
            return result(targetType, targetId, value, 0, false);
        }

        boolean selfVote = voterId.equals(authorId);
        int karmaDelta = selfVote ? 0 : value.getKarmaDelta();
        if (existingVote == null) {
            voteRepository.save(new Vote(voterId, targetType, targetId, value));
        } else {
            if (!selfVote) {
                karmaDelta -= existingVote.getValue().getKarmaDelta();
            }
            existingVote.setValue(value);
            voteRepository.save(existingVote);
        }

        int score = getScore(targetType, targetId);
        scoreUpdater.update(score);
        VoteResult result = new VoteResult(targetType, targetId, value, score, karmaDelta, true);
        publishVoteChange(voter, authorId, result);
        return result;
    }

    private VoteResult removeVote(Long voterId, VoteTargetType targetType, Long targetId, Long authorId,
                                  ScoreUpdater scoreUpdater) {
        User voter = userService.findById(voterId);
        Vote existingVote = voteRepository.findByVoterIdAndTargetTypeAndTargetId(voterId, targetType, targetId)
                .orElse(null);
        if (existingVote == null) {
            return result(targetType, targetId, null, 0, false);
        }

        int karmaDelta = voterId.equals(authorId)
                ? 0
                : -existingVote.getValue().getKarmaDelta();
        voteRepository.delete(existingVote);
        int score = getScore(targetType, targetId);
        scoreUpdater.update(score);
        VoteResult result = new VoteResult(targetType, targetId, null, score, karmaDelta, true);
        publishVoteChange(voter, authorId, result);
        return result;
    }

    private VoteResult result(VoteTargetType targetType, Long targetId, VoteValue currentVote,
                              int karmaDelta, boolean changed) {
        return new VoteResult(
                targetType,
                targetId,
                currentVote,
                getScore(targetType, targetId),
                karmaDelta,
                changed
        );
    }

    private int getScore(VoteTargetType targetType, Long targetId) {
        Objects.requireNonNull(targetId, "targetId must not be null");
        return voteRepository.calculateScore(targetType, targetId);
    }

    private Post requirePostVoteInput(Long voterId, Long postId, VoteValue value) {
        Objects.requireNonNull(voterId, "voterId must not be null");
        Objects.requireNonNull(postId, "postId must not be null");
        Objects.requireNonNull(value, "value must not be null");

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));
        userService.findById(voterId);
        return post;
    }

    private Comment requireCommentVoteInput(Long voterId, Long commentId, VoteValue value) {
        Objects.requireNonNull(voterId, "voterId must not be null");
        Objects.requireNonNull(commentId, "commentId must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (commentRepository == null) {
            throw new IllegalStateException("Comment voting is not configured");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found with ID: " + commentId));
        userService.findById(voterId);
        return comment;
    }

    @Transactional
    @Retry(name = "votePersistence")
    public void castVote(Long userId, Long postId, int delta) {
        if (delta == 0) {
            throw new IllegalArgumentException("Vote delta must not be zero");
        }
        voteOnPost(userId, postId, delta > 0 ? VoteValue.UPVOTE : VoteValue.DOWNVOTE);
    }

    private void publishVoteChange(User voter, Long authorId, VoteResult result) {
        if (eventPublisher != null) {
            eventPublisher.publish(new VoteCastEvent(
                    result.targetType(), result.targetId(), voter.getId(), authorId, voter.getUsername(),
                    result.currentVote(), result.karmaDelta(), result.score()));
        }
        if (uiBroadcaster == null) {
            return;
        }
        Runnable broadcast = () -> uiBroadcaster.broadcastVoteUpdate(
                result.targetType().name(), result.targetId(), result.score());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcast.run();
                }
            });
        } else {
            broadcast.run();
        }
    }

    @FunctionalInterface
    private interface ScoreUpdater {
        void update(int score);
    }
}