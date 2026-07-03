package com.redditclone.comments.controller;

import com.redditclone.comments.model.Comment;
import com.redditclone.comments.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;


    public record CreateCommentRequest(
            UUID postId,
            UUID userId,
            String content,
            UUID replyId   // null for a top-level comment, set for a reply
    ) {}

    @PostMapping("/create")
    public ResponseEntity<Comment> create(@RequestBody CreateCommentRequest request) {
        Comment comment = request.replyId() == null
                ? commentService.createComment(request.postId(), request.userId(), request.content())
                : commentService.replyComment(request.postId(), request.userId(), request.content(), request.replyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    // --- edit ---

    public record EditCommentRequest(String content) {}

    @PutMapping("/{commentId}")
    public ResponseEntity<Comment> edit(@PathVariable UUID commentId, @RequestBody EditCommentRequest request) {
        Comment updated = commentService.editComment(commentId, request.content());
        return ResponseEntity.ok(updated);
    }

    // --- delete ---

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    // --- ranking / sorting ---

    @GetMapping("/post/{postId}/controversial")
    public List<Comment> getMostControversial(@PathVariable UUID postId) {
        return commentService.rankCommentsOnPost_Contr(postId);
    }

    @GetMapping("/post/{postId}/votes")
    public List<Comment> getByVoteCount(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "true") boolean descending) {
        return commentService.rankCommentsOnPost_voteCount(postId, descending);
    }

    @GetMapping("/post/{postId}/recency")
    public List<Comment> getByRecency(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "true") boolean descending) {
        return commentService.rankCommentsOnPost_recency(postId, descending);
    }
}