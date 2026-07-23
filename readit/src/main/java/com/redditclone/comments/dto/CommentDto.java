package com.redditclone.comments.dto;

/**
 * Carries the fields needed to create a comment.
 * parentCommentId is null for a top-level comment, or set for a reply.
 */
public record CommentDto(Long postId, Long parentCommentId, String body) {

}