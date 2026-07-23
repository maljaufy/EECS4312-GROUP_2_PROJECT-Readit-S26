package com.redditclone.user.domain;

import com.redditclone.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_preferences", uniqueConstraints = {
        @UniqueConstraint(columnNames = "user_id")
})
@Getter
@Setter
public class NotificationPreference extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private boolean emailEnabled = true;

    @Column(nullable = false)
    private boolean pushEnabled = true;

    @Column(nullable = false)
    private boolean replyNotifications = true;

    @Column(nullable = false)
    private boolean mentionNotifications = true;

    @Column(nullable = false)
    private boolean voteNotifications = false; // Off by default

    @Column(nullable = false)
    private boolean moderationNotifications = true;

    @Column(nullable = false)
    private String emailFrequency = "IMMEDIATELY"; // IMMEDIATELY, DAILY, WEEKLY
}
