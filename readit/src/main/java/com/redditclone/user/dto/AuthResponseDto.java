package com.redditclone.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponseDto {
    private String token;
    private String username;
    private String email;
    private int karma;
    private boolean authenticated;
}
