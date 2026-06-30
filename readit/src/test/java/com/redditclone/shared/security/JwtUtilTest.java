package com.redditclone.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil Unit Tests")
public class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Inject test values (same as in application.yml)
        ReflectionTestUtils.setField(jwtUtil, "secret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 24 hours
    }

    @Test
    @DisplayName("Should generate token and extract username correctly")
    void generateToken_And_ExtractUsername_AreConsistent() {
        UserDetails userDetails = new User("marwan", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        String token = jwtUtil.generateToken(userDetails);
        assertNotNull(token);

        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals("marwan", extractedUsername);
    }

    @Test
    @DisplayName("Should validate token successfully for same user")
    void validateToken_ReturnsTrue_ForValidToken() {
        UserDetails userDetails = new User("marwan", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String token = jwtUtil.generateToken(userDetails);

        boolean isValid = jwtUtil.validateToken(token, userDetails);
        assertTrue(isValid);
    }

    @Test
    @DisplayName("Should invalidate token for different user")
    void validateToken_ReturnsFalse_ForDifferentUser() {
        UserDetails user1 = new User("marwan", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        UserDetails user2 = new User("other", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String token = jwtUtil.generateToken(user1);

        boolean isValid = jwtUtil.validateToken(token, user2);
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Should extract expiration date correctly")
    void extractExpiration_ReturnsValidDate() {
        UserDetails userDetails = new User("marwan", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        String token = jwtUtil.generateToken(userDetails);

        var expiration = jwtUtil.extractExpiration(token);
        assertNotNull(expiration);
        assertTrue(expiration.after(new java.util.Date()));
    }

    @Test
    @DisplayName("Should throw exception for malformed token")
    void extractUsername_ThrowsException_ForMalformedToken() {
        assertThrows(Exception.class, () -> jwtUtil.extractUsername("invalid.token.here"));
    }

    @Test
    @DisplayName("Should extract roles from token claims")
    void generateToken_ShouldContainRolesInClaims() {
        UserDetails userDetails = new User("marwan", "password",
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"),
                        new SimpleGrantedAuthority("ROLE_MODERATOR")
                ));
        String token = jwtUtil.generateToken(userDetails);
        String roles = jwtUtil.extractClaim(token, claims -> claims.get("roles", String.class));
        assertTrue(roles.contains("ROLE_USER") && roles.contains("ROLE_MODERATOR"));
    }
}
