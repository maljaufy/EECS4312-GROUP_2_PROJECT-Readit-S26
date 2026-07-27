package com.redditclone.posts.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record PostSummaryDto(
        Long id,
        String title,
        String content,
        String authorUsername,
        String subredditName,
        LocalDateTime createdAt
) implements Serializable {
}
