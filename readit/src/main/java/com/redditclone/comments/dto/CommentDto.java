package com.redditclone.comments.dto;

public record CommentDto(
        String body,
        Long parentCommentId
) {
}
