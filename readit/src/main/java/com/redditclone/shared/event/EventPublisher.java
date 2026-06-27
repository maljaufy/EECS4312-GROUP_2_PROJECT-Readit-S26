package com.redditclone.shared.event;

public interface EventPublisher {

    /*
    Event publisher interface: Abstract base for all event publishers
    */
    void publish(DomainEvent event);
}
