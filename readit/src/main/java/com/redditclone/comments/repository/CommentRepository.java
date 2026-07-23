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

    @Query("SELECT c FROM Comment c JOIN FETCH c.author " +
            "WHERE c.post = :post AND c.parentComment IS NULL " +
            "ORDER BY c.voteScore DESC, c.createdAt ASC")
    List<Comment> findBestTopLevelByPost(@Param("post") Post post);

    @Query("SELECT c FROM Comment c JOIN FETCH c.author " +
            "WHERE c.post = :post AND c.parentComment IS NULL " +
            "ORDER BY c.createdAt DESC")
    List<Comment> findNewestTopLevelByPost(@Param("post") Post post);

    /**
     * A controversial comment has substantial voting activity with the two
     * sides as balanced as possible.  The first expression is twice the
     * smaller of the up/down counts; total activity breaks ties.
     */
    @Query(value = """
            SELECT c.*
            FROM comments c
            LEFT JOIN votes v
              ON v.target_type = 'COMMENT' AND v.target_id = c.id
            WHERE c.post_id = :postId AND c.parent_comment_id IS NULL
            GROUP BY c.id
            ORDER BY
              (COUNT(v.id) - ABS(COALESCE(SUM(
                CASE v.vote_value WHEN 'UPVOTE' THEN 1 WHEN 'DOWNVOTE' THEN -1 ELSE 0 END
              ), 0))) DESC,
              COUNT(v.id) DESC,
              c.created_at ASC
            """, nativeQuery = true)
    List<Comment> findControversialTopLevelByPostId(@Param("postId") Long postId);

    @Query(value = """
            SELECT c.*
            FROM comments c
            WHERE c.post_id = :postId
              AND to_tsvector('english', COALESCE(c.body, ''))
                  @@ websearch_to_tsquery('english', :query)
            ORDER BY ts_rank_cd(
                       to_tsvector('english', COALESCE(c.body, '')),
                       websearch_to_tsquery('english', :query)
                     ) DESC,
                     c.created_at DESC
            """, nativeQuery = true)
    List<Comment> searchByPostId(@Param("postId") Long postId, @Param("query") String query);

    @Query(value = """
            SELECT c.*
            FROM comments c
            WHERE to_tsvector('english', COALESCE(c.body, ''))
                  @@ websearch_to_tsquery('english', :query)
            ORDER BY ts_rank_cd(
                       to_tsvector('english', COALESCE(c.body, '')),
                       websearch_to_tsquery('english', :query)
                     ) DESC,
                     c.created_at DESC
            """, nativeQuery = true)
    List<Comment> search(@Param("query") String query);

    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.parentComment = :parentComment ORDER BY c.createdAt ASC")
    List<Comment> findByParentCommentOrderByCreatedAtAsc(@Param("parentComment") Comment parentComment);

}
