package com.redditclone.user;

import com.redditclone.shared.test.TestcontainersBase;
import com.redditclone.user.domain.Role;
import com.redditclone.user.domain.User;
import com.redditclone.user.dto.UserProfileDto;
import com.redditclone.user.repository.UserRepository;
import com.redditclone.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@DisplayName("User Context Integration Tests")
public class UserContextIntegrationTest extends TestcontainersBase {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.findByUsername("integrationtest")
                .ifPresent(user -> userRepository.delete(user));

        testUser = userService.register("integrationtest", "integration@test.com", "password123");
    }

    @Test
    @DisplayName("Should register a new user successfully")
    void registerUser_Success() {
        User user = userService.register("newuser", "newuser@test.com", "password123");
        assertNotNull(user);
        assertNotNull(user.getId());
        assertEquals("newuser", user.getUsername());
        assertEquals("newuser@test.com", user.getEmail());
        assertTrue(passwordEncoder.matches("password123", user.getPasswordHash()));
        assertTrue(user.getRoles().contains(Role.USER));
        assertEquals(0, user.getKarma());
    }

    @Test
    @DisplayName("Should authenticate user with valid credentials")
    void authenticate_Success() {
        User authenticated = userService.authenticate("integrationtest", "password123");
        assertNotNull(authenticated);
        assertEquals("integrationtest", authenticated.getUsername());
    }

    @Test
    @DisplayName("Should find user by username")
    void findByUsername_ReturnsUser() {
        User found = userService.findByUsername("integrationtest");
        assertNotNull(found);
        assertEquals("integrationtest", found.getUsername());
        assertEquals("integration@test.com", found.getEmail());
    }

    @Test
    @DisplayName("Should retrieve user profile with stats")
    void getUserProfile_ReturnsProfileWithStats() {
        UserProfileDto profile = userService.getUserProfile("integrationtest");
        assertNotNull(profile);
        assertEquals("integrationtest", profile.getUsername());
        assertEquals(0, profile.getKarma());
        assertNotNull(profile.getJoinedAt());
    }

    @Test
    @DisplayName("Should promote user to moderator")
    void promoteToModerator_Success() {
        userService.promoteToModerator(testUser.getId());
        User updated = userService.findById(testUser.getId());
        assertTrue(updated.hasRole(Role.MODERATOR));
    }

    @Test
    @DisplayName("Should promote user to admin")
    void promoteToAdmin_Success() {
        userService.promoteToAdmin(testUser.getId());
        User updated = userService.findById(testUser.getId());
        assertTrue(updated.hasRole(Role.ADMIN));
    }

    @Test
    @DisplayName("Should update notification preferences")
    void updateNotificationPreferences_Success() {
        var prefs = userService.getNotificationPreferences(testUser.getId());
        assertNotNull(prefs);
        assertTrue(prefs.isEmailEnabled());

        var updated = userService.updateNotificationPreferences(
                testUser.getId(),
                false,  // emailEnabled
                true,   // pushEnabled
                false,  // replyNotifications
                true,   // mentionNotifications
                false,  // voteNotifications
                true,   // moderationNotifications
                "DAILY" // emailFrequency
        );

        assertFalse(updated.isEmailEnabled());
        assertEquals("DAILY", updated.getEmailFrequency());
    }
}
