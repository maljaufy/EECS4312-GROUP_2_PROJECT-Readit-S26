package com.redditclone.comments.service;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService Unit Tests")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private UserService userService;

    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentService = new CommentService(commentRepository, userService);
    }

    @Test
    @DisplayName("Should create a flat comment")
    void createComment_SavesTrimmedComment() {
        Comment savedComment = new Comment(10L, 1L, "Nice post");
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        Comment result = commentService.createComment(10L, 1L, "  Nice post  ");

        verify(userService).findById(1L);
        ArgumentCaptor<Comment> commentCaptor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(commentCaptor.capture());
        assertEquals(10L, commentCaptor.getValue().getPostId());
        assertEquals(1L, commentCaptor.getValue().getAuthorId());
        assertEquals("Nice post", commentCaptor.getValue().getBody());
        assertSame(savedComment, result);
    }

    @Test
    @DisplayName("Should reject blank comment body")
    void createComment_RejectsBlankBody() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> commentService.createComment(10L, 1L, "   ")
        );

        assertEquals("Comment body must not be blank", exception.getMessage());
        verifyNoInteractions(userService, commentRepository);
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
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return flat comments for a post")
    void findFlatCommentsForPost_ReturnsRepositoryResults() {
        List<Comment> comments = List.of(
                new Comment(10L, 1L, "First"),
                new Comment(10L, 2L, "Second")
        );
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(10L)).thenReturn(comments);

        List<Comment> result = commentService.findFlatCommentsForPost(10L);

        assertSame(comments, result);
        verify(commentRepository).findByPostIdOrderByCreatedAtAsc(10L);
    }
}
