package com.redditclone.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationPreferenceDto {

    private Long id;
    private boolean emailEnabled;
    private boolean pushEnabled;
    private boolean replyNotifications;
    private boolean mentionNotifications;
    private boolean voteNotifications;
    private boolean moderationNotifications;
    private String emailFrequency;
}
