package com.redditclone.posts.repository;

import com.redditclone.posts.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p JOIN FETCH p.author JOIN FETCH p.subreddit ORDER BY p.createdAt DESC")
    List<Post> findAllByOrderByCreatedAtDesc();

    @Query("SELECT p FROM Post p JOIN FETCH p.author JOIN FETCH p.subreddit WHERE p.id = :id")
    Optional<Post> findByIdWithAuthorAndSubreddit(@Param("id") Long id);
}
