package com.redditclone.comments.service;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.dto.CommentDto;
import com.redditclone.comments.event.CommentCreatedEvent;
import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.shared.event.EventPublisher;
import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class CommentService {

    @Autowired
    private EventPublisher eventPublisher;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserService userService;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository, UserService userService) {
            this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userService = userService;
    }

    @Transactional
    public Comment createComment(Long postId, Long authorId, String body) {
        return createComment(new CommentDto(postId, null, body), authorId);
    }

    @Transactional
    public Comment createComment(CommentDto dto, Long authorId) {
        Objects.requireNonNull(dto, "comment must not be null");
        Objects.requireNonNull(dto.getPostId(), "postId must not be null");
        Objects.requireNonNull(authorId, "authorId must not be null");
        String normalizedBody = normalizeBody(dto.getBody());

        User author = userService.findById(authorId);
        Post post = postRepository.findById(dto.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + dto.getPostId()));
        Comment comment = new Comment(post, author, normalizedBody);

        if (dto.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(dto.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent comment not found with ID: " + dto.getParentCommentId()));
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
        Objects.requireNonNull(dto, "comment must not be null");
        Objects.requireNonNull(author, "author must not be null");
        Objects.requireNonNull(post, "post must not be null");
        Comment comment = new Comment(post, author, normalizeBody(dto.getBody()));
        if (dto.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(dto.getParentCommentId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Parent comment not found with ID: " + dto.getParentCommentId()));
            if (!post.getId().equals(parent.getPostId())) {
                throw new IllegalArgumentException("Parent comment belongs to a different post");
            }
            comment.setParentComment(parent);
        }
        Comment saved = commentRepository.save(comment);

        publishCommentCreated(saved, post, author);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Comment> findFlatCommentsForPost(Long postId) {
        Objects.requireNonNull(postId, "postId must not be null");
        return commentRepository.findByPost_IdOrderByCreatedAtAsc(postId);
    }

    public List<Comment> findTopLevelByPost(Post post) {
        return commentRepository.findByPostAndParentCommentIsNullOrderByCreatedAtAsc(post);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId)
    {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Comment not found"));

        if (!comment.getAuthorId().equals(userId)) {
            throw new SecurityException(
                    "You cannot delete another user's comment"
            );
        }

        commentRepository.delete(comment);
    }

    public List<Comment> findReplies(Comment parentComment) {
        return commentRepository.findByParentCommentOrderByCreatedAtAsc(parentComment);
    }

    private String normalizeBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment body must not be blank");
        }
        return body.trim();
    }

    private void publishCommentCreated(Comment comment, Post post, User author) {
        if (eventPublisher != null) {
            eventPublisher.publish(new CommentCreatedEvent(
                    comment.getId(), post.getId(), author.getUsername(), comment.getBody(),
                    comment.getParentComment() == null ? null : comment.getParentComment().getId()));
        }
    }
}
