package com.redditclone.comments.service;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.domain.CommentSortOption;
import com.redditclone.comments.dto.CommentDto;
import com.redditclone.comments.event.CommentCreatedEvent;
import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.shared.event.EventPublisher;
import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class CommentService {

    @Autowired
    private EventPublisher eventPublisher;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserService userService;
    private final MeterRegistry meterRegistry;

    @Autowired
    public CommentService(CommentRepository commentRepository, PostRepository postRepository,
                          UserService userService, MeterRegistry meterRegistry) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userService = userService;
        this.meterRegistry = meterRegistry;
    }

    /** Convenience constructor retained for focused unit tests. */
    public CommentService(CommentRepository commentRepository, PostRepository postRepository, UserService userService) {
        this(commentRepository, postRepository, userService, Metrics.globalRegistry);
    }

    @Transactional
    public Comment createComment(Long postId, Long authorId, String body) {
        return timed("create", () -> createCommentInternal(new CommentDto(postId, null, body), authorId));
    }

    @Transactional
    public Comment createComment(CommentDto dto, Long authorId) {
        return timed("create", () -> createCommentInternal(dto, authorId));
    }

    private Comment createCommentInternal(CommentDto dto, Long authorId) {
        Objects.requireNonNull(dto, "comment must not be null");
        Objects.requireNonNull(dto.postId(), "postId must not be null");
        Objects.requireNonNull(authorId, "authorId must not be null");
        String normalizedBody = normalizeBody(dto.body());

        User author = userService.findById(authorId);
        Post post = postRepository.findById(dto.postId())
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + dto.postId()));
        Comment comment = new Comment(post, author, normalizedBody);

        if (dto.parentCommentId() != null) {
            Comment parent = commentRepository.findById(dto.parentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent comment not found with ID: " + dto.parentCommentId()));
            if (!post.getId().equals(parent.getPostId())) {
                throw new IllegalArgumentException("Parent comment belongs to a different post");
            }
            comment.setParentComment(parent);
        }

        Comment saved = commentRepository.save(comment);
        publishCommentCreated(saved, post, author);
        return saved;
    }

    @Transactional
    public Comment addComment(CommentDto dto, User author, Post post) {
        return timed("create", () -> {
            Objects.requireNonNull(dto, "comment must not be null");
            Objects.requireNonNull(author, "author must not be null");
            Objects.requireNonNull(post, "post must not be null");
            Comment comment = new Comment(post, author, normalizeBody(dto.body()));
            if (dto.parentCommentId() != null) {
                Comment parent = commentRepository.findById(dto.parentCommentId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Parent comment not found with ID: " + dto.parentCommentId()));
                if (!post.getId().equals(parent.getPostId())) {
                    throw new IllegalArgumentException("Parent comment belongs to a different post");
                }
                comment.setParentComment(parent);
            }
            Comment saved = commentRepository.save(comment);

            publishCommentCreated(saved, post, author);

            return saved;
        });
    }

    @Transactional(readOnly = true)
    public List<Comment> findFlatCommentsForPost(Long postId) {
        return timed("read-flat", () -> {
            Objects.requireNonNull(postId, "postId must not be null");
            return commentRepository.findByPost_IdOrderByCreatedAtAsc(postId);
        });
    }

    @Transactional(readOnly = true)
    public List<Comment> findTopLevelByPost(Post post) {
        return timed("rank", () -> commentRepository
                .findByPostAndParentCommentIsNullOrderByCreatedAtAsc(
                        Objects.requireNonNull(post, "post must not be null")));
    }

    @Transactional(readOnly = true)
    public List<Comment> findTopLevelByPost(Post post, CommentSortOption sortOption) {
        return timed("rank", () -> {
            Objects.requireNonNull(post, "post must not be null");
            Objects.requireNonNull(sortOption, "sortOption must not be null");
            List<Comment> comments = switch (sortOption) {
                case BEST -> commentRepository.findBestTopLevelByPost(post);
                case CONTROVERSIAL -> commentRepository.findControversialTopLevelByPostId(post.getId());
                case NEWEST -> commentRepository.findNewestTopLevelByPost(post);
            };
            initializeAuthors(comments);
            return comments;
        });
    }

    @Transactional(readOnly = true)
    public List<Comment> searchComments(Long postId, String query) {
        return timed("search", () -> {
            Objects.requireNonNull(postId, "postId must not be null");
            String normalizedQuery = normalizeQuery(query);
            List<Comment> comments = commentRepository.searchByPostId(postId, normalizedQuery);
            initializeAuthors(comments);
            return comments;
        });
    }

    @Transactional(readOnly = true)
    public List<Comment> searchComments(String query) {
        return timed("search", () -> {
            List<Comment> comments = commentRepository.search(normalizeQuery(query));
            initializeAuthors(comments);
            return comments;
        });
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId)
    {
        timed("delete", () -> {
            Comment comment = commentRepository.findById(commentId)
                    .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

            if (!comment.getAuthorId().equals(userId)) {
                throw new SecurityException("You cannot delete another user's comment");
            }

            commentRepository.delete(comment);
            return null;
        });
    }

    @Transactional(readOnly = true)
    public List<Comment> findReplies(Comment parentComment) {
        return timed("read-replies", () -> commentRepository
                .findByParentCommentOrderByCreatedAtAsc(parentComment));
    }

    private String normalizeBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment body must not be blank");
        }
        return body.trim();
    }

    private String normalizeQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query must not be blank");
        }
        return query.trim();
    }

    private void initializeAuthors(List<Comment> comments) {
        comments.forEach(comment -> comment.getAuthor().getUsername());
    }

    private <T> T timed(String operation, Supplier<T> action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            return action.get();
        } catch (RuntimeException exception) {
            outcome = "error";
            throw exception;
        } finally {
            sample.stop(Timer.builder("readit.comments.latency")
                    .description("Latency of comment-context operations")
                    .tag("operation", operation)
                    .tag("outcome", outcome)
                    .register(meterRegistry));
        }
    }

    private void publishCommentCreated(Comment comment, Post post, User author) {
        if (eventPublisher != null) {
            eventPublisher.publish(new CommentCreatedEvent(
                    comment.getId(), post.getId(), author.getUsername(), comment.getBody(),
                    comment.getParentComment() == null ? null : comment.getParentComment().getId()));
        }
    }
}
