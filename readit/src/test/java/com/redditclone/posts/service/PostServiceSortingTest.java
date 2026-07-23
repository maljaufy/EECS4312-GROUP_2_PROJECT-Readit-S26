package com.redditclone.posts.service;

import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.domain.PostSortOption;
import com.redditclone.posts.dto.PostSummaryDto;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.service.SubredditService;
import com.redditclone.user.domain.User;
import com.redditclone.voting.repository.VoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostService Feed Sorting Tests")
class PostServiceSortingTest {

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

    private Post oldPopular;
    private Post newestUnpopular;
    private Post middling;

    @BeforeEach
    void setUp() {
        User author = new User();
        author.setId(1L);
        author.setUsername("amr");

        Subreddit subreddit = new Subreddit("programming", "desc", false);
        subreddit.setId(5L);

        oldPopular = new Post("Old but popular", "body", author, subreddit);
        oldPopular.setId(1L);
        oldPopular.setCreatedAt(LocalDateTime.now().minusDays(3));
        oldPopular.setVoteScore(100);

        middling = new Post("Middling", "body", author, subreddit);
        middling.setId(2L);
        middling.setCreatedAt(LocalDateTime.now().minusDays(1));
        middling.setVoteScore(10);

        newestUnpopular = new Post("Brand new", "body", author, subreddit);
        newestUnpopular.setId(3L);
        newestUnpopular.setCreatedAt(LocalDateTime.now());
        newestUnpopular.setVoteScore(0);
    }

    @Test
    @DisplayName("NEW sorting puts the most recent post first")
    void newSortingReturnsNewestFirst() {
        when(postRepository.findAllWithDetails())
                .thenReturn(List.of(oldPopular, middling, newestUnpopular));

        List<PostSummaryDto> feed = postService.getFeed(PostSortOption.NEW);

        assertEquals("Brand new", feed.get(0).title());
        assertEquals("Middling", feed.get(1).title());
        assertEquals("Old but popular", feed.get(2).title());
    }

    @Test
    @DisplayName("TOP sorting puts the highest scoring post first")
    void topSortingReturnsHighestScoreFirst() {
        when(postRepository.findAllWithDetails())
                .thenReturn(List.of(newestUnpopular, middling, oldPopular));

        List<PostSummaryDto> feed = postService.getFeed(PostSortOption.TOP);

        assertEquals("Old but popular", feed.get(0).title());
        assertEquals("Middling", feed.get(1).title());
        assertEquals("Brand new", feed.get(2).title());
    }

    @Test
    @DisplayName("HOT sorting favours a recent post over an older one with the same score")
    void hotSortingFavoursRecentPosts() {
        middling.setVoteScore(10);
        oldPopular.setVoteScore(10);
        when(postRepository.findAllWithDetails())
                .thenReturn(List.of(oldPopular, middling));

        List<PostSummaryDto> feed = postService.getFeed(PostSortOption.HOT);

        assertEquals("Middling", feed.get(0).title());
    }

    @Test
    @DisplayName("the default feed still returns newest first")
    void defaultFeedIsNewest() {
        when(postRepository.findAllWithDetails())
                .thenReturn(List.of(oldPopular, newestUnpopular));

        List<PostSummaryDto> feed = postService.getFeed();

        assertEquals("Brand new", feed.get(0).title());
    }
}
