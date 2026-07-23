package com.redditclone.notification.repository;

import com.redditclone.notification.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    Optional<ProcessedEvent> findByEventIdAndHandlerName(String eventId, String handlerName);
    boolean existsByEventIdAndHandlerName(String eventId, String handlerName);
    long countByEventIdAndHandlerName(String eventId, String handlerName);
}
