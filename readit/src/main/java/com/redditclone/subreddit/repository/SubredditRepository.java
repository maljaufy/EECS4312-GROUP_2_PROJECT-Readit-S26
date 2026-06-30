package com.redditclone.subreddit.repository;

import com.redditclone.subreddit.domain.Subreddit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubredditRepository extends JpaRepository<Subreddit, Long> {

    boolean existsByNameIgnoreCase(String name);

    Optional<Subreddit> findByNameIgnoreCase(String name);
}
