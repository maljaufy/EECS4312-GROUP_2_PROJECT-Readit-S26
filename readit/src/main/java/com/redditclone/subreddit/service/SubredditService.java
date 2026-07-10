package com.redditclone.subreddit.service;

import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.repository.SubredditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubredditService {

    private final SubredditRepository repository;

    public SubredditService(SubredditRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Subreddit create(String name, String description, boolean isPrivate) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new IllegalArgumentException("Subreddit name cannot be empty.");
        }
        if (repository.existsByNameIgnoreCase(cleanName)) {
            throw new IllegalArgumentException("A subreddit named '" + cleanName + "' already exists.");
        }
        return repository.save(new Subreddit(cleanName, description, isPrivate));
    }

    @Transactional(readOnly = true)
    public List<Subreddit> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Subreddit getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Subreddit not found."));
    }

    @Transactional(readOnly = true)
    public List<Subreddit> searchSubreddits(String query) {
        if (query == null || query.trim().isEmpty()) {
            return findAll();
        }
        String searchQuery = query.trim().toLowerCase();
        return repository.findAll().stream()
                .filter(s -> s.getName().toLowerCase().contains(searchQuery) ||
                           (s.getDescription() != null && s.getDescription().toLowerCase().contains(searchQuery)))
                .toList();
    }
}
