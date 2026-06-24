package com.redditclone.user.dto;

import com.redditclone.user.domain.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserProfileDto {
    private Long id;
    private String username;
    private String email;
    private int karma;
    private String bio;
    private String profileImageUrl;
    private Set<Role> roles;
    private LocalDateTime joinedAt;
    private long postCount;
    private long commentCount;
}
