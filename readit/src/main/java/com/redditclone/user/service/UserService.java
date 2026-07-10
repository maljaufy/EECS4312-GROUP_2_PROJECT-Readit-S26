package com.redditclone.user.service;

import com.redditclone.shared.event.EventPublisher;
import com.redditclone.user.domain.Role;
import com.redditclone.user.domain.User;
import com.redditclone.user.dto.UserProfileDto;
import com.redditclone.user.event.UserRegisteredEvent;
import com.redditclone.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    /*
    User service: Main user service: User business logic
    i.e. Service for User
    */

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EventPublisher eventPublisher;

    /**
     * Registers a new user.
     *
     * @param username the username
     * @param email    the email
     * @param password the raw password
     * @return the registered user
     * @throws RuntimeException if username or email already exists
     */
    @Transactional
    public User register(String username, String email, String password) {
        // Validate uniqueness
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        // Create user
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRoles(Set.of(Role.USER));

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", username);

        // Publish event for downstream consumers (karma calculation, analytics, etc.)
        eventPublisher.publish(new UserRegisteredEvent(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail()
        ));

        return savedUser;
    }

    /**
     * Authenticates a user and returns the user details.
     *
     * @param username the username
     * @param password the raw password
     * @return the authenticated user
     * @throws AuthenticationException if authentication fails
     */
    public User authenticate(String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found after authentication"));
    }

    /**
     * Finds a user by username.
     *
     * @param username the username
     * @return the user
     * @throws IllegalArgumentException if user not found
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /**
     * Finds a user by ID.
     *
     * @param userId the user ID
     * @return the user
     * @throws IllegalArgumentException if user not found
     */
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
    }

    /**
     * Updates a user's karma.
     *
     * @param userId the user ID
     * @param delta  the karma change (positive or negative)
     */
    @Transactional
    public void updateKarma(Long userId, int delta) {
        User user = findById(userId);
        user.addKarma(delta);
        userRepository.save(user);
        log.debug("Updated karma for user {}: +{} (now {})", user.getUsername(), delta, user.getKarma());
    }

    /**
     * Gets a user's profile data.
     *
     * @param username the username
     * @return the profile DTO
     */
    public UserProfileDto getUserProfile(String username) {
        User user = findByUsername(username);
        // TODO: Uncomment when Post entity is implemented
        // long postCount = userRepository.countPostsByUserId(user.getId());
        // TODO: Uncomment when Comment entity is implemented
        // long commentCount = userRepository.countCommentsByUserId(user.getId());

        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .karma(user.getKarma())
                .bio(user.getBio())
                .profileImageUrl(user.getProfileImageUrl())
                .roles(user.getRoles())
                .joinedAt(user.getCreatedAt())
                .postCount(0) // TODO: Replace with actual count when Post entity is implemented
                .commentCount(0) // TODO: Replace with actual count when Comment entity is implemented
                .build();
    }

    /**
     * Updates a user's profile.
     *
     * @param userId        the user ID
     * @param bio           the new bio
     * @param profileImageUrl the new profile image URL
     * @return the updated profile DTO
     */
    @Transactional
    public UserProfileDto updateProfile(Long userId, String bio, String profileImageUrl) {
        User user = findById(userId);
        if (bio != null) {
            user.setBio(bio);
        }
        if (profileImageUrl != null) {
            user.setProfileImageUrl(profileImageUrl);
        }
        User updatedUser = userRepository.save(user);
        return getUserProfile(updatedUser.getUsername());
    }

    /**
     * Checks if a username is available.
     *
     * @param username the username to check
     * @return true if available, false otherwise
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Checks if an email is available.
     *
     * @param email the email to check
     * @return true if available, false otherwise
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * Gets the currently authenticated user.
     *
     * @return the current user
     * @throws IllegalStateException if no user is authenticated
     */
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new IllegalStateException("No user is currently logged in.");
        }
        return findByUsername(auth.getName());
    }

    /**
     * Promotes a user to moderator.
     *
     * @param userId the user ID
     */
    @Transactional
    public void promoteToModerator(Long userId) {
        User user = findById(userId);
        user.addRole(Role.MODERATOR);
        userRepository.save(user);
        log.info("Promoted user {} to moderator", user.getUsername());
    }

    /**
     * Promotes a user to admin.
     *
     * @param userId the user ID
     */
    @Transactional
    public void promoteToAdmin(Long userId) {
        User user = findById(userId);
        user.addRole(Role.ADMIN);
        userRepository.save(user);
        log.info("Promoted user {} to admin", user.getUsername());
    }

}
