package com.redditclone.comments.dto;

/**
 * Carries the fields needed to create a comment.
 * parentCommentId is null for a top-level comment, or set for a reply.
 */
public class CommentDto {

    private final Long postId;
    private final Long parentCommentId;
    private final String body;

    public CommentDto(Long postId, Long parentCommentId, String body) {
        this.postId = postId;
        this.parentCommentId = parentCommentId;
        this.body = body;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getParentCommentId() {
        return parentCommentId;
    }

    public String getBody() {
        return body;
    }
}