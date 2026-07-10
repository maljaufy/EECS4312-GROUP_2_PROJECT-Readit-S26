package com.redditclone.posts.service;

import com.redditclone.posts.domain.Post;
import com.redditclone.posts.dto.PostDto;
import com.redditclone.posts.dto.PostSummaryDto;
import com.redditclone.posts.event.PostCreatedEvent;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.shared.event.EventPublisher;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.service.SubredditService;
import com.redditclone.user.domain.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostService {
    @Autowired
    private EventPublisher eventPublisher;
    private final PostRepository postRepository;
    private final SubredditService subredditService;


    public PostService(PostRepository postRepository, SubredditService subredditService) {
        this.postRepository = postRepository;
        this.subredditService = subredditService;
    }

    @Transactional
    public Post createPost(PostDto dto, User author) {
        String cleanTitle = dto.title() == null ? "" : dto.title().trim();
        if (cleanTitle.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }
        Subreddit subreddit = subredditService.getById(dto.subredditId());
        Post post = new Post(cleanTitle, dto.content(), author, subreddit);
        Post saved = postRepository.save(post);

        // Publish event
        eventPublisher.publish(new PostCreatedEvent(
            saved.getId(),
            saved.getTitle(),
            author.getUsername(),
            saved.getSubreddit() != null ? saved.getSubreddit().getName() : "general"
        ));

        return saved;
    }

    @Transactional(readOnly = true)
    public List<PostSummaryDto> getFeed() {
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .map(p -> new PostSummaryDto(
                        p.getId(),
                        p.getTitle(),
                        p.getContent(),
                        p.getAuthor().getUsername(),
                        p.getSubreddit().getName(),
                        p.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PostSummaryDto> searchPosts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getFeed();
        }
        String searchQuery = query.trim().toLowerCase();
        return postRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                .filter(p -> p.getTitle().toLowerCase().contains(searchQuery) || 
                           (p.getContent() != null && p.getContent().toLowerCase().contains(searchQuery)))
                .map(p -> new PostSummaryDto(
                        p.getId(),
                        p.getTitle(),
                        p.getContent(),
                        p.getAuthor().getUsername(),
                        p.getSubreddit().getName(),
                        p.getCreatedAt()))
                .toList();
    }
}
