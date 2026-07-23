package com.redditclone.posts.service;

import com.redditclone.posts.domain.Post;
import com.redditclone.posts.dto.PostDto;
import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.service.SubredditService;
import com.redditclone.user.domain.User;
import com.redditclone.voting.repository.VoteRepository;
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
@DisplayName("PostService Unit Tests")
class PostServiceTest {

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

    @Test
    @DisplayName("creates a post when the title is valid")
    void createsPostWithValidData() {
        Long subredditId = 1L;
        Subreddit subreddit = new Subreddit("programming", "desc", false);
        User author = new User();
        when(subredditService.getById(subredditId)).thenReturn(subreddit);
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        PostDto dto = new PostDto("Hello world", "My first post", subredditId);
        Post post = postService.createPost(dto, author);

        assertEquals("Hello world", post.getTitle());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("rejects an empty title")
    void rejectsEmptyTitle() {
        User author = new User();
        PostDto dto = new PostDto("   ", "body", 1L);
        assertThrows(IllegalArgumentException.class,
                () -> postService.createPost(dto, author));
        verify(postRepository, never()).save(any());
    }
}
