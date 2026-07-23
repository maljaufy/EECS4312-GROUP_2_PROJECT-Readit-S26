package com.redditclone.user.repository;

import com.redditclone.user.domain.Role;
import com.redditclone.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("UserRepository Integration Tests")
@TestPropertySource(properties = "spring.sql.init.enabled=false")
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User savedUser;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("marwan");
        user.setEmail("marwan@example.com");
        user.setPasswordHash("encoded");
        user.setRoles(Set.of(Role.USER));
        user.setKarma(0);

        savedUser = userRepository.save(user);
        entityManager.flush();
        entityManager.clear(); // Detach to avoid stale data
    }

    @Test
    @DisplayName("Should find user by username")
    void findByUsername_ReturnsUser_WhenExists() {
        Optional<User> found = userRepository.findByUsername("marwan");
        assertTrue(found.isPresent());
        assertEquals("marwan", found.get().getUsername());
        assertEquals("marwan@example.com", found.get().getEmail());
        assertEquals(0, found.get().getKarma());
        assertTrue(found.get().getRoles().contains(Role.USER));
    }

    @Test
    @DisplayName("Should return empty when username not found")
    void findByUsername_ReturnsEmpty_WhenNotFound() {
        Optional<User> found = userRepository.findByUsername("notexists");
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should find user by email")
    void findByEmail_ReturnsUser_WhenExists() {
        Optional<User> found = userRepository.findByEmail("marwan@example.com");
        assertTrue(found.isPresent());
        assertEquals("marwan", found.get().getUsername());
    }

    @Test
    @DisplayName("Should return empty when email not found")
    void findByEmail_ReturnsEmpty_WhenNotFound() {
        Optional<User> found = userRepository.findByEmail("notfound@example.com");
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Should return true when username exists")
    void existsByUsername_ReturnsTrue_WhenExists() {
        boolean exists = userRepository.existsByUsername("marwan");
        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false when username does not exist")
    void existsByUsername_ReturnsFalse_WhenNotFound() {
        boolean exists = userRepository.existsByUsername("unknown");
        assertFalse(exists);
    }

    @Test
    @DisplayName("Should return true when email exists")
    void existsByEmail_ReturnsTrue_WhenExists() {
        boolean exists = userRepository.existsByEmail("marwan@example.com");
        assertTrue(exists);
    }

    @Test
    @DisplayName("Should return false when email does not exist")
    void existsByEmail_ReturnsFalse_WhenNotFound() {
        boolean exists = userRepository.existsByEmail("unknown@example.com");
        assertFalse(exists);
    }

    @Test
    @DisplayName("Should correctly count posts for a user")
    void countPostsByUserId_ReturnsCorrectCount() {
        long count = userRepository.countPostsByUserId(savedUser.getId());
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Should correctly count comments for a user")
    void countCommentsByUserId_ReturnsCorrectCount() {
        long count = userRepository.countCommentsByUserId(savedUser.getId());
        assertEquals(0, count);
    }

    @Test
    @DisplayName("Should find karma by user ID")
    void findKarmaByUserId_ReturnsKarma() {
        int karma = userRepository.findKarmaByUserId(savedUser.getId());
        assertEquals(0, karma);
    }

    @Test
    @DisplayName("Should save user with optimistic locking version")
    void saveUser_ShouldIncrementVersionOnUpdate() {
        User user = userRepository.findById(savedUser.getId()).orElseThrow();
        Long initialVersion = user.getVersion();
        user.setBio("New bio");
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        User updated = userRepository.findById(savedUser.getId()).orElseThrow();
        assertEquals(initialVersion + 1, updated.getVersion());
    }
}
