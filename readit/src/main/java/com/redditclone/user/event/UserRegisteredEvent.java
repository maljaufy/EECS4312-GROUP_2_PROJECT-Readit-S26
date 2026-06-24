package com.redditclone.user.event;

import com.redditclone.shared.event.DomainEvent;
import lombok.Getter;

@Getter
public class UserRegisteredEvent extends DomainEvent {

    /*
        Event published when user registers
     */

    private final Long userId;
    private final String username;
    private final String email;

    public UserRegisteredEvent(Long userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    @Override
    public String getAggregateId() {
        return String.valueOf(userId);
    }

    @Override
    public String getEventType() {
        return "UserRegistered";
    }
}
