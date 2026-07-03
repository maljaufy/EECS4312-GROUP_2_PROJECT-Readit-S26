package com.redditclone.comments.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import com.redditclone.comments.model.Comment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Comment findByCommentId(UUID commentId);

    @Query("SELECT c FROM Comment c WHERE c.postId = :postId " +
            "ORDER BY (SIZE(c.downvotes) * 1.0 / (CASE WHEN SIZE(c.upvotes) = 0 THEN 1 ELSE SIZE(c.upvotes) END)) DESC")
    List<Comment> findMostContreversial(@Param("postId") UUID postId);
    @Query("SELECT c FROM Comment c WHERE c.postId = :postId ORDER BY (SIZE(c.upvotes) - SIZE(c.downvotes) ) DESC")
    List<Comment> findMostPopularComment(@Param("postId") UUID postId);
    @Query("SELECT c FROM Comment c WHERE c.postId = :postId ORDER BY (SIZE(c.upvotes) - SIZE(c.downvotes) ) ASC")
    List<Comment> findLeastPopularComment(@Param("postId") UUID postId);
    @Query("SELECT c FROM Comment c WHERE c.postId = :postId ORDER BY (c.createdAt) DESC")
    List<Comment> findMostRecentComment(@Param("postId") UUID postId);
    @Query("SELECT c FROM Comment c WHERE c.postId = :postId ORDER BY (c.createdAt) ASC")
    List<Comment> findOldestComment(@Param("postId") UUID postId);

}