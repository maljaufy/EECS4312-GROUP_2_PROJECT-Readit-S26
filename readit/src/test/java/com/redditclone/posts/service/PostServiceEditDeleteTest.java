package com.redditclone.posts.service;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.service.SubredditService;
import com.redditclone.user.domain.User;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService Edit and Delete Tests")
class PostServiceEditDeleteTest {

    private static final Long AUTHOR_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long POST_ID = 10L;

    @Mock
    private PostRepository postRepository;

    @Mock
    private SubredditService subredditService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private VoteRepository voteRepository;

    @InjectMocks
    private PostService postService;

    private Post post;

    @BeforeEach
    void setUp() {
        User author = new User();
        author.setId(AUTHOR_ID);
        author.setUsername("amr");

        Subreddit subreddit = new Subreddit("programming", "desc", false);
        subreddit.setId(5L);

        post = new Post("Original title", "Original body", author, subreddit);
        post.setId(POST_ID);
    }

    @Test
    @DisplayName("author can update their own post")
    void authorCanUpdatePost() {
        when(postRepository.findByIdWithAuthorAndSubreddit(POST_ID)).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        Post updated = postService.updatePost(POST_ID, "New title", "New body", AUTHOR_ID);

        assertEquals("New title", updated.getTitle());
        assertEquals("New body", updated.getContent());
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("a different user cannot update someone else's post")
    void nonAuthorCannotUpdatePost() {
        when(postRepository.findByIdWithAuthorAndSubreddit(POST_ID)).thenReturn(Optional.of(post));

        assertThrows(IllegalStateException.class,
                () -> postService.updatePost(POST_ID, "Hijacked", "body", OTHER_USER_ID));
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("updating with an empty title is rejected")
    void updateRejectsEmptyTitle() {
        when(postRepository.findByIdWithAuthorAndSubreddit(POST_ID)).thenReturn(Optional.of(post));

        assertThrows(IllegalArgumentException.class,
                () -> postService.updatePost(POST_ID, "   ", "body", AUTHOR_ID));
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleting a post removes its comments and votes first")
    void deleteRemovesCommentsAndVotes() {
        Comment comment = mock(Comment.class);
        when(comment.getId()).thenReturn(99L);
        when(postRepository.findByIdWithAuthorAndSubreddit(POST_ID)).thenReturn(Optional.of(post));
        when(commentRepository.findByPost_IdOrderByCreatedAtAsc(POST_ID)).thenReturn(List.of(comment));

        postService.deletePost(POST_ID, AUTHOR_ID);

        verify(voteRepository).deleteByTargetTypeAndTargetId(VoteTargetType.COMMENT, 99L);
        verify(commentRepository).delete(comment);
        verify(voteRepository).deleteByTargetTypeAndTargetId(VoteTargetType.POST, POST_ID);
        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("a different user cannot delete someone else's post")
    void nonAuthorCannotDeletePost() {
        when(postRepository.findByIdWithAuthorAndSubreddit(POST_ID)).thenReturn(Optional.of(post));

        assertThrows(IllegalStateException.class,
                () -> postService.deletePost(POST_ID, OTHER_USER_ID));
        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    @DisplayName("a logged out user cannot delete a post")
    void loggedOutUserCannotDeletePost() {
        when(postRepository.findByIdWithAuthorAndSubreddit(POST_ID)).thenReturn(Optional.of(post));

        assertThrows(IllegalStateException.class,
                () -> postService.deletePost(POST_ID, null));
        verify(postRepository, never()).delete(any(Post.class));
    }
}
