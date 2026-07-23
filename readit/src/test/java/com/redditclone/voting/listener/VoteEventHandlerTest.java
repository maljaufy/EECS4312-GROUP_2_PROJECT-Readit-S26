package com.redditclone.voting.listener;

import com.redditclone.notification.domain.ProcessedEvent;
import com.redditclone.notification.repository.ProcessedEventRepository;
import com.redditclone.shared.push.UIBroadcaster;
import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;
import com.redditclone.voting.event.VoteEventMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Idempotent vote event handler tests")
class VoteEventHandlerTest {

    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private UserService userService;
    @Mock private UIBroadcaster uiBroadcaster;

    private VoteEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new VoteEventHandler(processedEventRepository, userService, uiBroadcaster);
    }

    @Test
    @DisplayName("Ignores an event already reserved by this handler")
    void ignoresDuplicateEvent() {
        VoteEventMessage event = event("event-1");
        when(processedEventRepository.existsByEventIdAndHandlerName(
                "event-1", VoteEventHandler.HANDLER_NAME)).thenReturn(true);

        assertFalse(handler.handle(event));

        verify(processedEventRepository, never()).saveAndFlush(any());
        verifyNoInteractions(userService, uiBroadcaster);
    }

    @Test
    @DisplayName("Reserves a new event before applying karma")
    void appliesNewEventOnce() {
        VoteEventMessage event = event("event-2");
        User author = new User();
        author.setId(2L);
        author.setUsername("author");
        author.setKarma(1);
        when(userService.findById(2L)).thenReturn(author);
        when(processedEventRepository.saveAndFlush(any(ProcessedEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(handler.handle(event));

        verify(processedEventRepository).saveAndFlush(any(ProcessedEvent.class));
        verify(userService).updateKarma(2L, 1);
        verify(uiBroadcaster).broadcast(any());
    }

    private VoteEventMessage event(String eventId) {
        return new VoteEventMessage(
                eventId, LocalDateTime.now(), VoteTargetType.POST, 10L,
                1L, 2L, "voter", VoteValue.UPVOTE, 1, 1);
    }
}
