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
        Objects.requireNonNull(postId, "postId must not be null");
        Objects.requireNonNull(authorId, "authorId must not be null");

        String normalizedBody = normalizeBody(body);
        User author = userService.findById(authorId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with ID: " + postId));

        return commentRepository.save(new Comment(post, author, normalizedBody));
    }

    @Transactional
    public Comment addComment(CommentDto dto, User author, Post post) {
        String normalizedBody = normalizeBody(dto.body());
        Comment comment = new Comment(post, author, normalizedBody);
        Comment saved = commentRepository.save(comment);

        eventPublisher.publish(new CommentCreatedEvent(
                saved.getId(),
                saved.getPost().getId(),
                author.getUsername(),
                saved.getBody(),
                saved.getParentComment() != null ? saved.getParentComment().getId() : null
        ));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Comment> findFlatCommentsForPost(Long postId) {
        Objects.requireNonNull(postId, "postId must not be null");
        return commentRepository.findByPost_IdOrderByCreatedAtAsc(postId);
    }

    private String normalizeBody(String body) {
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment body must not be blank");
        }
        return body.trim();
    }
}
