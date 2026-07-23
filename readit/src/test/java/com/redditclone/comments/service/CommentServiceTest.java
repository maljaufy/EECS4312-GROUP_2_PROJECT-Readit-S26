package com.redditclone.comments.service;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.domain.CommentSortOption;
import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Unit Tests")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserService userService;

    private CommentService commentService;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        commentService = new CommentService(commentRepository, postRepository, userService, meterRegistry);
    }

    @Test
    @DisplayName("Should create a flat comment")
    void createComment_SavesTrimmedComment() {
        Post post = postWithId(10L);
        User author = userWithId(1L);
        Comment savedComment = new Comment(post, author, "Nice post");
        when(userService.findById(1L)).thenReturn(author);
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        Comment result = commentService.createComment(10L, 1L, "  Nice post  ");

        verify(userService).findById(1L);
        verify(postRepository).findById(10L);
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertEquals(10L, commentCaptor.getValue().getPostId());
        assertEquals(1L, commentCaptor.getValue().getAuthorId());
        assertEquals("Nice post", commentCaptor.getValue().getBody());
        assertSame(savedComment, result);
        assertEquals(1, meterRegistry.get("readit.comments.latency")
                .tag("operation", "create").tag("outcome", "success").timer().count());
    }

    @Test
    @DisplayName("Should reject blank comment body")
    void createComment_RejectsBlankBody() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentService.createComment(10L, 1L, "   ")
        );

        assertEquals("Comment body must not be blank", exception.getMessage());
        verifyNoInteractions(userService, postRepository, commentRepository);
        assertEquals(1, meterRegistry.get("readit.comments.latency")
                .tag("operation", "create").tag("outcome", "error").timer().count());
    }

    @Test
    @DisplayName("Should propagate missing author failure")
    void createComment_PropagatesMissingAuthorFailure() {
        when(userService.findById(1L)).thenThrow(new IllegalArgumentException("User not found with ID: 1"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentService.createComment(10L, 1L, "Nice post")
        );

        assertEquals("User not found with ID: 1", exception.getMessage());
        verifyNoInteractions(postRepository);
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject comments on missing posts")
    void createComment_RejectsMissingPost() {
        User author = userWithId(1L);
        when(userService.findById(1L)).thenReturn(author);
        when(postRepository.findById(10L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentService.createComment(10L, 1L, "Nice post")
        );

        assertEquals("Post not found with ID: 10", exception.getMessage());
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return flat comments for a post")
    void findFlatCommentsForPost_ReturnsRepositoryResults() {
        Post post = postWithId(10L);
        List<Comment> comments = List.of(
                new Comment(post, userWithId(1L), "First"),
                new Comment(post, userWithId(2L), "Second")
        );
        when(commentRepository.findByPost_IdOrderByCreatedAtAsc(10L)).thenReturn(comments);

        List<Comment> result = commentService.findFlatCommentsForPost(10L);

        assertSame(comments, result);
        verify(commentRepository).findByPost_IdOrderByCreatedAtAsc(10L);
    }

    @Test
    @DisplayName("Should delegate controversial ranking to the vote-aware query")
    void findTopLevelByPost_UsesControversialRanking() {
        Post post = postWithId(10L);
        Comment comment = new Comment(post, userWithId(1L), "Debatable");
        when(commentRepository.findControversialTopLevelByPostId(10L)).thenReturn(List.of(comment));

        List<Comment> result = commentService.findTopLevelByPost(post, CommentSortOption.CONTROVERSIAL);

        assertEquals(List.of(comment), result);
        verify(commentRepository).findControversialTopLevelByPostId(10L);
    }

    @Test
    @DisplayName("Should trim and execute a post-scoped full-text search")
    void searchComments_UsesNormalizedQuery() {
        Post post = postWithId(10L);
        Comment comment = new Comment(post, userWithId(1L), "PostgreSQL search is useful");
        when(commentRepository.searchByPostId(10L, "PostgreSQL search")).thenReturn(List.of(comment));

        List<Comment> result = commentService.searchComments(10L, "  PostgreSQL search  ");

        assertEquals(List.of(comment), result);
        verify(commentRepository).searchByPostId(10L, "PostgreSQL search");
        assertEquals(1, meterRegistry.get("readit.comments.latency")
                .tag("operation", "search").tag("outcome", "success").timer().count());
    }

    private Post postWithId(Long id) {
        Post post = new Post();
        post.setId(id);
        return post;
    }

    private User userWithId(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }
}
