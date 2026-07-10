package com.redditclone.posts.service;

import com.redditclone.posts.domain.Post;
import com.redditclone.posts.dto.PostSummaryDto;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.service.SubredditService;
import com.redditclone.user.domain.User;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostService {

    private final PostRepository postRepository;
    private final SubredditService subredditService;

    public PostService(PostRepository postRepository, SubredditService subredditService) {
        this.postRepository = postRepository;
        this.subredditService = subredditService;
    }

    @Transactional
    public Post createPost(String title, String content, Long subredditId, User author) {
        String cleanTitle = title == null ? "" : title.trim();
        if (cleanTitle.isEmpty()) {
            throw new IllegalArgumentException("Title cannot be empty.");
        }
        Subreddit subreddit = subredditService.getById(subredditId);
        Post post = new Post(cleanTitle, content, author, subreddit);
        return postRepository.save(post);
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
    @Transactional
    public Post getPostById(Long postId) {
        return postRepository.findByIdWithAuthorAndSubreddit(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + postId));
    }
}
