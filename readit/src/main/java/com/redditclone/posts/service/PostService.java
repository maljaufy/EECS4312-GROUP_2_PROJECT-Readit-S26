package com.redditclone.posts.service;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.domain.PostSortOption;
import com.redditclone.posts.dto.PostDto;
import com.redditclone.posts.dto.PostSummaryDto;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.shared.config.CacheConfig;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.service.SubredditService;
import com.redditclone.user.domain.User;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.repository.VoteRepository;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final SubredditService subredditService;
    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository;
    private final ObservationRegistry observationRegistry;

    public PostService(PostRepository postRepository,
                       SubredditService subredditService,
                       CommentRepository commentRepository,
                       VoteRepository voteRepository,
                       ObservationRegistry observationRegistry) {
        this.postRepository = postRepository;
        this.subredditService = subredditService;
        this.commentRepository = commentRepository;
        this.voteRepository = voteRepository;
        this.observationRegistry = observationRegistry;
    }

    @Transactional
    @CacheEvict(value = CacheConfig.POST_FEED_CACHE, allEntries = true)
    public Post createPost(String title, String content, Long subredditId, User author) {
        String cleanTitle = title == null ? "" : title.trim();
        if (cleanTitle.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }
        Subreddit subreddit = subredditService.getById(subredditId);
        Post post = new Post(cleanTitle, content, author, subreddit);
        return postRepository.save(post);
    }

    /** Compatibility entry point for the existing Vaadin view and service tests. */
    @Transactional
    @CacheEvict(value = CacheConfig.POST_FEED_CACHE, allEntries = true)
    public Post createPost(PostDto dto, User author) {
        if (dto == null) {
            throw new IllegalArgumentException("Post must not be null.");
        }
        return createPost(dto.title(), dto.content(), dto.subredditId(), author);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.POST_FEED_CACHE, allEntries = true)
    public Post updatePost(Long postId, String title, String content, Long editorId) {
        Post post = requirePost(postId);
        requireAuthor(post, editorId);

        String cleanTitle = title == null ? "" : title.trim();
        if (cleanTitle.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }

        post.setTitle(cleanTitle);
        post.setContent(content);
        return postRepository.save(post);
    }

    @Transactional
    @CacheEvict(value = CacheConfig.POST_FEED_CACHE, allEntries = true)
    public void deletePost(Long postId, Long requesterId) {
        Post post = requirePost(postId);
        requireAuthor(post, requesterId);

        List<Comment> comments = commentRepository.findByPost_IdOrderByCreatedAtAsc(postId).stream()
                .sorted(Comparator.comparing(Comment::getId).reversed())
                .toList();

        for (Comment comment : comments) {
            voteRepository.deleteByTargetTypeAndTargetId(VoteTargetType.COMMENT, comment.getId());
            commentRepository.delete(comment);
            commentRepository.flush();
        }

        voteRepository.deleteByTargetTypeAndTargetId(VoteTargetType.POST, postId);
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public boolean isAuthor(Long postId, Long userId) {
        if (userId == null) {
            return false;
        }
        return requirePost(postId).getAuthor().getId().equals(userId);
    }

    @Cacheable(value = CacheConfig.POST_FEED_CACHE, key = "'NEW'")
    @Transactional(readOnly = true)
    public List<PostSummaryDto> getFeed() {
        return loadFeed(PostSortOption.NEW);
    }

    @Cacheable(value = CacheConfig.POST_FEED_CACHE, key = "#sort == null ? 'NEW' : #sort.name()")
    @Transactional(readOnly = true)
    public List<PostSummaryDto> getFeed(PostSortOption sort) {
        return loadFeed(sort == null ? PostSortOption.NEW : sort);
    }

    /**
     * The read side of the feed. Runs only on a cache miss, so this database
     * read is what a Zipkin trace and the feed latency metric measure. On a
     * cache hit the whole method is skipped and the feed comes back from Redis.
     */
    private List<PostSummaryDto> loadFeed(PostSortOption option) {
        return io.micrometer.observation.Observation
                .createNotStarted("readit.posts.feed", observationRegistry)
                .lowCardinalityKeyValue("sort", option.name())
                .observe(() -> postRepository.findAllWithDetails().stream()
                        .sorted(comparatorFor(option))
                        .map(PostService::toSummary)
                        .toList());
    }

    @Transactional(readOnly = true)
    public List<PostSummaryDto> searchPosts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getFeed();
        }
        String searchQuery = query.trim().toLowerCase();
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(post -> post.getTitle().toLowerCase().contains(searchQuery)
                        || (post.getContent() != null
                        && post.getContent().toLowerCase().contains(searchQuery)))
                .map(PostService::toSummary)
                .toList();
    }

    @Transactional
    public Post getPostById(Long postId) {
        return requirePost(postId);
    }

    @Transactional(readOnly = true)
    public List<PostSummaryDto> getPostsFromLast24Hours() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(p -> p.getCreatedAt().isAfter(twentyFourHoursAgo))
                .map(PostService::toSummary)
                .toList();
    }

    private Post requirePost(Long postId) {
        return postRepository.findByIdWithAuthorAndSubreddit(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
    }

    private void requireAuthor(Post post, Long userId) {
        if (userId == null) {
            throw new IllegalStateException("You need to be logged in to do that.");
        }
        if (!post.getAuthor().getId().equals(userId)) {
            throw new IllegalStateException("You can only edit or delete your own posts.");
        }
    }

    private static Comparator<Post> comparatorFor(PostSortOption option) {
        Comparator<Post> newest = Comparator.comparing(Post::getCreatedAt).reversed();
        return switch (option) {
            case NEW -> newest;
            case TOP -> Comparator.comparingInt(Post::getVoteScore).reversed().thenComparing(newest);
            case HOT -> Comparator.comparingDouble(PostService::hotRank).reversed().thenComparing(newest);
        };
    }

    private static double hotRank(Post post) {
        int score = post.getVoteScore();
        double magnitude = Math.log10(Math.max(Math.abs(score), 1));
        int direction = Integer.compare(score, 0);
        long ageSeconds = post.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
        return magnitude + (direction * ageSeconds) / 45000.0;
    }

    private static PostSummaryDto toSummary(Post post) {
        return new PostSummaryDto(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor().getUsername(),
                post.getSubreddit().getName(),
                post.getCreatedAt());
    }
}
