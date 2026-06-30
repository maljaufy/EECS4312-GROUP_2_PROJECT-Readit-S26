package com.redditclone.voting.service;

import com.redditclone.user.service.UserService;
import com.redditclone.voting.domain.Vote;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoteService Unit Tests")
class VoteServiceTest {

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private UserService userService;

    private VoteService voteService;

    @BeforeEach
    void setUp() {
        voteService = new VoteService(voteRepository, userService);
    }

    @Test
    @DisplayName("Should create an upvote and add one karma")
    void upvotePost_CreatesVoteAndAddsKarma() {
        when(voteRepository.findByVoterIdAndTargetTypeAndTargetId(1L, VoteTargetType.POST, 10L))
                .thenReturn(Optional.empty());
        when(voteRepository.calculateScore(VoteTargetType.POST, 10L)).thenReturn(1);

        VoteResult result = voteService.upvotePost(1L, 10L, 2L);

        ArgumentCaptor<Vote> voteCaptor = ArgumentCaptor.forClass(Vote.class);
        verify(voteRepository).save(voteCaptor.capture());
        assertEquals(1L, voteCaptor.getValue().getVoterId());
        assertEquals(VoteTargetType.POST, voteCaptor.getValue().getTargetType());
        assertEquals(10L, voteCaptor.getValue().getTargetId());
        assertEquals(VoteValue.UPVOTE, voteCaptor.getValue().getValue());

        verify(userService).updateKarma(2L, 1);
        assertEquals(VoteValue.UPVOTE, result.currentVote());
        assertEquals(1, result.score());
        assertEquals(1, result.karmaDelta());
        assertTrue(result.changed());
    }

    @Test
    @DisplayName("Should switch downvote to upvote and add two karma")
    void upvotePost_SwitchesExistingDownvote() {
        Vote existingVote = new Vote(1L, VoteTargetType.POST, 10L, VoteValue.DOWNVOTE);
        when(voteRepository.findByVoterIdAndTargetTypeAndTargetId(1L, VoteTargetType.POST, 10L))
                .thenReturn(Optional.of(existingVote));
        when(voteRepository.calculateScore(VoteTargetType.POST, 10L)).thenReturn(1);

        VoteResult result = voteService.upvotePost(1L, 10L, 2L);

        assertEquals(VoteValue.UPVOTE, existingVote.getValue());
        verify(voteRepository).save(existingVote);
        verify(userService).updateKarma(2L, 2);
        assertEquals(2, result.karmaDelta());
        assertTrue(result.changed());
    }

    @Test
    @DisplayName("Should leave repeated same vote unchanged")
    void voteOnPost_RepeatedSameVoteIsIdempotent() {
        Vote existingVote = new Vote(1L, VoteTargetType.POST, 10L, VoteValue.UPVOTE);
        when(voteRepository.findByVoterIdAndTargetTypeAndTargetId(1L, VoteTargetType.POST, 10L))
                .thenReturn(Optional.of(existingVote));
        when(voteRepository.calculateScore(VoteTargetType.POST, 10L)).thenReturn(1);

        VoteResult result = voteService.upvotePost(1L, 10L, 2L);

        verify(voteRepository, never()).save(any());
        verify(userService, never()).updateKarma(anyLong(), anyInt());
        assertEquals(0, result.karmaDelta());
        assertFalse(result.changed());
    }

    @Test
    @DisplayName("Should remove existing vote and reverse karma")
    void removePostVote_RemovesVoteAndReversesKarma() {
        Vote existingVote = new Vote(1L, VoteTargetType.POST, 10L, VoteValue.DOWNVOTE);
        when(voteRepository.findByVoterIdAndTargetTypeAndTargetId(1L, VoteTargetType.POST, 10L))
                .thenReturn(Optional.of(existingVote));
        when(voteRepository.calculateScore(VoteTargetType.POST, 10L)).thenReturn(0);

        VoteResult result = voteService.removePostVote(1L, 10L, 2L);

        verify(voteRepository).delete(existingVote);
        verify(userService).updateKarma(2L, 1);
        assertNull(result.currentVote());
        assertEquals(1, result.karmaDelta());
        assertTrue(result.changed());
    }

    @Test
    @DisplayName("Should reject votes on own posts")
    void voteOnPost_RejectsSelfVote() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> voteService.downvotePost(1L, 10L, 1L)
        );

        assertEquals("Users cannot vote on their own posts", exception.getMessage());
        verifyNoInteractions(voteRepository, userService);
    }
}
