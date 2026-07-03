package com.redditclone.notification.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_event", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "handler_name"})
})
@Getter
@Setter
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "handler_name", nullable = false)
    private String handlerName;

    @Column(nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now();
}
