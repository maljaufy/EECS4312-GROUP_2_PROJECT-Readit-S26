package com.redditclone.user.repository;

import com.redditclone.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /*
    User repository: Main user repository: User repository
    i.e JPA repository for User
    */

    Optional<User> findByUsername(String username);
}
