package com.redditclone.notification.repository;

import com.redditclone.notification.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' ORDER BY o.createdAt ASC")
    List<OutboxEvent> findTop100ByStatusPending();

    @Query("SELECT o FROM OutboxEvent o WHERE o.status = 'PENDING' AND o.createdAt < :cutoff ORDER BY o.createdAt ASC")
    List<OutboxEvent> findPendingOlderThan(@Param("cutoff") java.time.LocalDateTime cutoff);
}
