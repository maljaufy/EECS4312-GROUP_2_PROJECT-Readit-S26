package com.redditclone.voting.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redditclone.notification.domain.ProcessedEvent;
import com.redditclone.notification.repository.ProcessedEventRepository;
import com.redditclone.shared.push.UIBroadcaster;
import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import com.redditclone.voting.event.VoteEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Kafka entry point for vote side effects.  Kafka is intentionally configured
 * for at-least-once delivery; {@link VoteEventHandler} supplies exactly-once
 * application semantics with the processed-events ledger.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VoteEventListener {

    private final ObjectMapper objectMapper;
    private final VoteEventHandler voteEventHandler;

    @KafkaListener(
            topics = "vote.events",
            groupId = "${readit.voting.consumer.group-id:readit-voting-karma}")
    public void handleVoteCast(String payload) {
        try {
            VoteEventMessage event = objectMapper.readValue(payload, VoteEventMessage.class);
            voteEventHandler.handle(event);
        } catch (DataIntegrityViolationException duplicateReservation) {
            log.debug("Vote event was reserved concurrently and is already being processed", duplicateReservation);
        } catch (JsonProcessingException invalidEvent) {
            throw new IllegalArgumentException("Invalid VoteCastEvent payload", invalidEvent);
        }
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
class VoteEventHandler {

    static final String HANDLER_NAME = "voting-karma-handler";

    private final ProcessedEventRepository processedEventRepository;
    private final UserService userService;
    private final UIBroadcaster uiBroadcaster;

    @Transactional
    public boolean handle(VoteEventMessage event) {
        validate(event);
        if (processedEventRepository.existsByEventIdAndHandlerName(event.eventId(), HANDLER_NAME)) {
            log.debug("Ignoring duplicate vote event {}", event.eventId());
            return false;
        }

        // Flush the unique reservation before applying any side effect. A
        // concurrent duplicate therefore fails while the whole transaction is
        // still safe to roll back.
        processedEventRepository.saveAndFlush(new ProcessedEvent(event.eventId(), HANDLER_NAME));
        if (event.delta() != 0) {
            userService.updateKarma(event.authorId(), event.delta());
        }

        User author = userService.findById(event.authorId());
        afterCommit(() -> uiBroadcaster.broadcast(ui -> ui.getPage().executeJs(
                "window.dispatchEvent(new CustomEvent('readit-karma-updated', " +
                        "{ detail: { username: $0, karma: $1 } }));",
                author.getUsername(), author.getKarma())));
        log.debug("Processed vote event {} for author {}", event.eventId(), event.authorId());
        return true;
    }

    private void validate(VoteEventMessage event) {
        if (event == null || event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException("Vote eventId must not be blank");
        }
        if (event.authorId() == null) {
            throw new IllegalArgumentException("Vote authorId must not be null");
        }
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
