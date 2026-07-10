package com.redditclone.posts.dto;

public record PostDto(
        String title,
        String content,
        Long subredditId
) {
}
