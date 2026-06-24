package com.redditclone.shared.event;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public abstract class DomainEvent {
    /*
    Base class for domain events: Abstract base for all events
    */

    private final String eventId;
    private final LocalDateTime occurredAt;

    public DomainEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.occurredAt = LocalDateTime.now();
    }

    public DomainEvent(String eventId, LocalDateTime occurredAt) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
    }

    public abstract String getAggregateId();
    public abstract String getEventType();


}
