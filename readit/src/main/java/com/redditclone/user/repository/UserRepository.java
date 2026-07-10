package com.redditclone.user.repository;

import com.redditclone.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /*
    User repository: Main user repository: User repository
    i.e JPA repository for User
    */

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("SELECT u.karma FROM User u WHERE u.id = :userId")
    int findKarmaByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.author.id = :userId")
    long countPostsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.author.id = :userId")
    long countCommentsByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(p.voteScore), 0) FROM Post p WHERE p.author.id = :userId")
    int getPostVoteScoreByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(c.voteScore), 0) FROM Comment c WHERE c.author.id = :userId")
    int getCommentVoteScoreByUserId(@Param("userId") Long userId);
}
