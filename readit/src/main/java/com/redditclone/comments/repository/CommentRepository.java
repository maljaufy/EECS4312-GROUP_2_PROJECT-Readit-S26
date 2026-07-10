package com.redditclone.comments.repository;

import com.redditclone.comments.domain.Comment;
import com.redditclone.posts.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByPost_IdOrderByCreatedAtAsc(Long postId);
    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.post = :post AND c.parentComment IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findByPostAndParentCommentIsNullOrderByCreatedAtAsc(@Param("post") Post post);

    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.parentComment = :parentComment ORDER BY c.createdAt ASC")
    List<Comment> findByParentCommentOrderByCreatedAtAsc(@Param("parentComment") Comment parentComment);

}
