package com.redditclone.voting.service;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.shared.event.EventPublisher;
import com.redditclone.shared.push.UIBroadcaster;
import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import com.redditclone.voting.domain.Vote;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.event.VoteCastEvent;
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
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserService userService;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private UIBroadcaster uiBroadcaster;

    private VoteService voteService;

    @BeforeEach
    void setUp() {
        voteService = new VoteService(voteRepository, postRepository, commentRepository,
                userService, eventPublisher, uiBroadcaster);
    }

    @Test
    @DisplayName("Should create an upvote and add one karma")
    void upvotePost_CreatesVoteAndAddsKarma() {
        givenPostWithAuthor(10L, 2L);
        givenVoter(1L);
        when(voteRepository.findByVoterIdAndTargetTypeAndTargetId(1L, VoteTargetType.POST, 10L))
                .thenReturn(Optional.empty());
        when(voteRepository.calculateScore(VoteTargetType.POST, 10L)).thenReturn(1);

        VoteResult result = voteService.upvotePost(1L, 10L);

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
        givenPostWithAuthor(10L, 2L);
        givenVoter(1L);
        Vote existingVote = new Vote(1L, VoteTargetType.POST, 10L, VoteValue.DOWNVOTE);
        when(voteRepository.findByVoterIdAndTargetTypeAndTargetId(1L, VoteTargetType.POST, 10L))
                .thenReturn(Optional.of(existingVote));
        when(voteRepository.calculateScore(VoteTargetType.POST, 10L)).thenReturn(1);

        VoteResult result = voteService.upvotePost(1L, 10L);

        assertEquals(VoteValue.UPVOTE, existingVote.getValue());
        verify(voteRepository).save(existingVote);
        verify(userService).updateKarma(2L, 2);
        assertEquals(2, result.karmaDelta());
        assertTrue(result.changed());
    }

    @Test
    @DisplayName("Should leave repeated same vote unchanged")
    void voteOnPost_RepeatedSameVoteIsIdempotent() {
        givenPostWithAuthor(10L, 2L);
        givenVoter(1L);
        Vote existingVote = new Vote(1L, VoteTargetType.POST, 10L, VoteValue.UPVOTE);
        when(voteRepository.findByVoterIdAndTargetTypeAndTargetId(1L, VoteTargetType.POST, 10L))
                .thenReturn(Optional.of(existingVote));
        when(voteRepository.calculateScore(VoteTargetType.POST, 10L)).thenReturn(1);

        VoteResult result = voteService.upvotePost(1L, 10L);

        verify(voteRepository, never()).save(any());
        verify(userService, never()).updateKarma(anyLong(), anyInt());
        assertEquals(0, result.karmaDelta());
        assertFalse(result.changed());
    }

    @Test
    @DisplayName("Should remove existing vote and reverse karma")
    void removePostVote_RemovesVoteAndReversesKarma() {
        givenPostWithAuthor(10L, 2L);
        givenVoter(1L);
        Vote existingVote = new Vote(1L, VoteTargetType.POST, 10L, VoteValue.DOWNVOTE);
        when(voteRepository.findByVoterIdAndTargetTypeAndTargetId(1L, VoteTargetType.POST, 10L))
                .thenReturn(Optional.of(existingVote));
        when(voteRepository.calculateScore(VoteTargetType.POST, 10L)).thenReturn(0);

        VoteResult result = voteService.removePostVote(1L, 10L);

        verify(voteRepository).delete(existingVote);
        verify(userService).updateKarma(2L, 1);
        assertNull(result.currentVote());
        assertEquals(1, result.karmaDelta());
        assertTrue(result.changed());
    }

    @Test
    @DisplayName("Should reject votes on own posts")
    void voteOnPost_RejectsSelfVote() {
        givenPostWithAuthor(10L, 1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> voteService.downvotePost(1L, 10L)
        );

        assertEquals("Users cannot vote on their own posts", exception.getMessage());
        verifyNoInteractions(voteRepository, userService);
    }

    @Test
    @DisplayName("Should reject votes on missing posts")
    void voteOnPost_RejectsMissingPost() {
        when(postRepository.findById(10L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> voteService.upvotePost(1L, 10L)
        );

        assertEquals("Post not found with ID: 10", exception.getMessage());
        verifyNoInteractions(voteRepository, userService);
    }

    @Test
    @DisplayName("Should vote on a comment, update its score, and broadcast after the change")
    void upvoteComment_UpdatesCommentKarmaAndBroadcasts() {
        Comment comment = givenCommentWithAuthor(20L, 2L);
        givenVoter(1L);
        when(voteRepository.findByVoterIdAndTargetTypeAndTargetId(1L, VoteTargetType.COMMENT, 20L))
                .thenReturn(Optional.empty());
        when(voteRepository.calculateScore(VoteTargetType.COMMENT, 20L)).thenReturn(1);

        VoteResult result = voteService.upvoteComment(1L, 20L);

        assertEquals(VoteTargetType.COMMENT, result.targetType());
        assertEquals(1, comment.getVoteScore());
        verify(userService).updateKarma(2L, 1);
        ArgumentCaptor<VoteCastEvent> eventCaptor = ArgumentCaptor.forClass(VoteCastEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(VoteTargetType.COMMENT, eventCaptor.getValue().getTargetType());
        assertEquals(20L, eventCaptor.getValue().getTargetId());
        verify(uiBroadcaster).broadcastVoteUpdate("COMMENT", 20L, 1);
    }

    @Test
    @DisplayName("Should reject votes on a user's own comment")
    void voteOnComment_RejectsSelfVote() {
        givenCommentWithAuthor(20L, 1L);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> voteService.downvoteComment(1L, 20L)
        );

        assertEquals("Users cannot vote on their own comments", exception.getMessage());
        verifyNoInteractions(voteRepository, userService, eventPublisher, uiBroadcaster);
    }

    private void givenPostWithAuthor(Long postId, Long authorId) {
        Post post = new Post();
        post.setId(postId);
        post.setAuthor(userWithId(authorId));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
    }

    private void givenVoter(Long voterId) {
        when(userService.findById(voterId)).thenReturn(userWithId(voterId));
    }

    private User userWithId(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Post postWithId(Long id) {
        Post post = new Post();
        post.setId(id);
        return post;
    }

    private Comment givenCommentWithAuthor(Long commentId, Long authorId) {
        Comment comment = new Comment(postWithId(10L), userWithId(authorId), "A comment");
        comment.setId(commentId);
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        return comment;
    }
}
