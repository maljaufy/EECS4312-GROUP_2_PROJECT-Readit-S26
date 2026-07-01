package com.redditclone.comments.service;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class CommentService {

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
