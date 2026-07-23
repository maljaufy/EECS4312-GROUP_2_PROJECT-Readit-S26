package com.redditclone.user.service;

import com.redditclone.shared.event.EventPublisher;
import com.redditclone.user.domain.Role;
import com.redditclone.user.domain.User;
import com.redditclone.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("marwan");
        mockUser.setEmail("marwan@example.com");
        mockUser.setPasswordHash("encodedPassword");
        mockUser.setKarma(0);
        mockUser.setRoles(new HashSet<>(Set.of(Role.USER)));
    }

    // ==================== REGISTRATION TESTS ====================

    @Test
    @DisplayName("Should register a new user successfully")
    void register_Success() {
        // Arrange
        String username = "marwan";
        String email = "marwan@example.com";
        String rawPassword = "password123";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        User registeredUser = userService.register(username, email, rawPassword);

        // Assert
        assertNotNull(registeredUser);
        assertEquals("marwan", registeredUser.getUsername());
        assertEquals("encodedPassword", registeredUser.getPasswordHash());
        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    @DisplayName("Should throw exception when username is already taken")
    void register_ThrowsException_WhenUsernameTaken() {
        // Arrange
        String username = "marwan";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register(username, "email@test.com", "pass"));
        assertEquals("Username already taken: marwan", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("Should throw exception when email is already registered")
    void register_ThrowsException_WhenEmailTaken() {
        // Arrange
        String email = "marwan@example.com";
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.register("newuser", email, "pass"));
        assertEquals("Email already registered: marwan@example.com", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    // ==================== FIND BY USERNAME TESTS ====================

    @Test
    @DisplayName("Should return user when username exists")
    void findByUsername_ReturnsUser_WhenExists() {
        // Arrange
        when(userRepository.findByUsername("marwan")).thenReturn(Optional.of(mockUser));

        // Act
        User found = userService.findByUsername("marwan");

        // Assert
        assertNotNull(found);
        assertEquals("marwan", found.getUsername());
        assertEquals("marwan@example.com", found.getEmail());
    }

    @Test
    @DisplayName("Should throw exception when username does not exist")
    void findByUsername_ThrowsException_WhenUserNotFound() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.findByUsername("unknown"));
    }

    // ==================== KARMA TESTS ====================

    @Test
    @DisplayName("Should update user karma successfully")
    void updateKarma_Success() {
        // Arrange
        Long userId = 1L;
        int delta = 10;
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        userService.updateKarma(userId, delta);

        // Assert
        assertEquals(10, mockUser.getKarma());
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("Should throw exception when updating karma for non-existent user")
    void updateKarma_ThrowsException_WhenUserNotFound() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.updateKarma(userId, 5));
    }

    // ==================== PROFILE TESTS ====================

    @Test
    @DisplayName("Should return user profile when user exists")
    void getUserProfile_ReturnsProfile_WhenUserExists() {
        // Arrange
        when(userRepository.findByUsername("marwan")).thenReturn(Optional.of(mockUser));
        when(userRepository.countPostsByUserId(1L)).thenReturn(5L);
        when(userRepository.countCommentsByUserId(1L)).thenReturn(10L);

        // Act
        var profile = userService.getUserProfile("marwan");

        // Assert
        assertNotNull(profile);
        assertEquals("marwan", profile.getUsername());
        assertEquals(0, profile.getKarma());
        assertEquals(5, profile.getPostCount());
        assertEquals(10, profile.getCommentCount());
    }

    // ==================== AVAILABILITY TESTS ====================

    @Test
    @DisplayName("Should return true when username is available")
    void isUsernameAvailable_ReturnsTrue_WhenNotTaken() {
        // Arrange
        when(userRepository.existsByUsername("available")).thenReturn(false);

        // Act & Assert
        assertTrue(userService.isUsernameAvailable("available"));
    }

    @Test
    @DisplayName("Should return false when username is taken")
    void isUsernameAvailable_ReturnsFalse_WhenTaken() {
        // Arrange
        when(userRepository.existsByUsername("marwan")).thenReturn(true);

        // Act & Assert
        assertFalse(userService.isUsernameAvailable("marwan"));
    }

    // ==================== PROMOTION TESTS ====================

    @Test
    @DisplayName("Should promote user to moderator successfully")
    void promoteToModerator_Success() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        userService.promoteToModerator(userId);

        // Assert
        assertTrue(mockUser.hasRole(Role.MODERATOR));
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("Should promote user to admin successfully")
    void promoteToAdmin_Success() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        // Act
        userService.promoteToAdmin(userId);

        // Assert
        assertTrue(mockUser.hasRole(Role.ADMIN));
        verify(userRepository).save(mockUser);
    }

    @Test
    @DisplayName("Should find user by ID when exists")
    void findById_ReturnsUser_WhenExists() {
        // Arrange
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // Act
        User found = userService.findById(userId);

        // Assert
        assertNotNull(found);
        assertEquals("marwan", found.getUsername());
    }

    @Test
    @DisplayName("Should throw exception when finding user by ID that doesn't exist")
    void findById_ThrowsException_WhenUserNotFound() {
        // Arrange
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userService.findById(userId));
    }

}
