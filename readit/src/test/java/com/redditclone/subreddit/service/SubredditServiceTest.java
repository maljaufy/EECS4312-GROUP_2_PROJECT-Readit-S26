package com.redditclone.subreddit.service;

import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.repository.SubredditRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubredditService Unit Tests")
class SubredditServiceTest {

    @Mock
    private SubredditRepository repository;

    @InjectMocks
    private SubredditService service;

    @Test
    @DisplayName("creates a subreddit when the name is valid and unique")
    void createsSubreddit() {
        when(repository.existsByNameIgnoreCase("programming")).thenReturn(false);
        when(repository.save(any(Subreddit.class))).thenAnswer(inv -> inv.getArgument(0));

        Subreddit result = service.create("programming", "All things code", false);

        assertEquals("programming", result.getName());
        verify(repository).save(any(Subreddit.class));
    }

    @Test
    @DisplayName("rejects an empty name")
    void rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class,
                () -> service.create("   ", "desc", false));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("rejects a duplicate name")
    void rejectsDuplicateName() {
        when(repository.existsByNameIgnoreCase("programming")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> service.create("programming", "desc", false));
        verify(repository, never()).save(any());
    }
}
