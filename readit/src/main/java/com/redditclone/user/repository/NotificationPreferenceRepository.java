package com.redditclone.user.repository;

import com.redditclone.user.domain.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import com.redditclone.user.domain.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface  NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUser(User user);

    Optional<NotificationPreference> findByUserId(Long userId);
}
