package com.redditclone.posts;

import com.redditclone.posts.domain.Post;
import com.redditclone.posts.domain.PostSortOption;
import com.redditclone.posts.dto.PostSummaryDto;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.posts.service.PostService;
import com.redditclone.shared.config.CacheConfig;
import com.redditclone.shared.test.TestcontainersBase;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.repository.SubredditRepository;
import com.redditclone.user.domain.User;
import com.redditclone.user.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = "spring.cache.type=redis")
@DisplayName("Post context Redis cache and tracing integration tests")
class PostContextIntegrationTest extends TestcontainersBase {

    @Autowired private PostService postService;
    @Autowired private PostRepository postRepository;
    @Autowired private SubredditRepository subredditRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CacheManager cacheManager;
    @Autowired private MeterRegistry meterRegistry;

    private User author;
    private Subreddit subreddit;

    @BeforeEach
    void setUp() {
        clearFeedCache();
        postRepository.deleteAll();
        author = saveUser("feed-author");
        subreddit = subredditRepository.save(new Subreddit("feed-integration", "Feed fixtures", false));
    }

    @Test
    @DisplayName("Uses a Redis-backed cache manager for the feed")
    void feedCacheIsRedisBacked() {
        Cache cache = cacheManager.getCache(CacheConfig.POST_FEED_CACHE);
        assertNotNull(cache);
        assertTrue(cache.getNativeCache().getClass().getName().toLowerCase().contains("redis"),
                "Expected a Redis-backed cache but was " + cache.getNativeCache().getClass().getName());
    }

    @Test
    @DisplayName("Materialises the feed into Redis on first load")
    void firstLoadPopulatesRedis() {
        postRepository.save(new Post("First post", "Body", author, subreddit));

        Cache cache = cacheManager.getCache(CacheConfig.POST_FEED_CACHE);
        assertNull(cache.get(PostSortOption.NEW.name()));

        List<PostSummaryDto> feed = postService.getFeed(PostSortOption.NEW);
        assertEquals(1, feed.size());

        Cache.ValueWrapper cached = cache.get(PostSortOption.NEW.name());
        assertNotNull(cached, "Feed should be stored in Redis after the first load");
        @SuppressWarnings("unchecked")
        List<PostSummaryDto> cachedFeed = (List<PostSummaryDto>) cached.get();
        assertEquals("First post", cachedFeed.getFirst().title());
    }

    @Test
    @DisplayName("Serves the second request from Redis without hitting the database again")
    void secondLoadComesFromCache() {
        postRepository.save(new Post("Cached post", "Body", author, subreddit));

        postService.getFeed(PostSortOption.NEW);
        double loadsAfterFirst = feedLoadCount();

        postService.getFeed(PostSortOption.NEW);
        double loadsAfterSecond = feedLoadCount();

        assertEquals(loadsAfterFirst, loadsAfterSecond,
                "Second call should be served from Redis, so no extra feed load should run");
    }

    @Test
    @DisplayName("Evicts the cached feed when a new post is created")
    void createEvictsCache() {
        postService.getFeed(PostSortOption.NEW);
        Cache cache = cacheManager.getCache(CacheConfig.POST_FEED_CACHE);
        assertNotNull(cache.get(PostSortOption.NEW.name()));

        postService.createPost("Fresh post", "Body", subreddit.getId(), author);

        assertNull(cache.get(PostSortOption.NEW.name()),
                "Creating a post should evict the stale feed from Redis");

        List<PostSummaryDto> feed = postService.getFeed(PostSortOption.NEW);
        assertEquals(1, feed.size());
        assertEquals("Fresh post", feed.getFirst().title());
    }

    @Test
    @DisplayName("Records a feed latency metric for observability")
    void recordsFeedMetric() {
        postRepository.save(new Post("Observed post", "Body", author, subreddit));

        postService.getFeed(PostSortOption.NEW);

        assertTrue(feedLoadCount() >= 1,
                "A feed load should be timed under the readit.posts.feed metric");
    }

    private double feedLoadCount() {
        return meterRegistry.find("readit.posts.feed").timers().stream()
                .mapToLong(timer -> timer.count())
                .sum();
    }

    private void clearFeedCache() {
        Cache cache = cacheManager.getCache(CacheConfig.POST_FEED_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("not-used-in-integration-test");
        return userRepository.save(user);
    }
}
