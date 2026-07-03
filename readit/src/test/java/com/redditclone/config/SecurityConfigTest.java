package com.redditclone.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@DisplayName("Security Configuration Integration Tests")
public class SecurityConfigTest {
    @Autowired
    private MockMvc mockMvc;

    // Mock Vaadin views if they cause issues; but we can just test endpoints.
    // For endpoints that are not mapped, we get 404; but that's fine.

    @Test
    @DisplayName("Public endpoints should be accessible without authentication")
    void publicEndpoints_ShouldBeAccessible() throws Exception {
        // /register and /log in are permitAll
        mockMvc.perform(get("http://localhost:8081/register"))
                .andExpect(status().isOk()); // Should return Vaadin view if not redirected
        mockMvc.perform(get("http://localhost:8081/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected endpoints should be denied without authentication")
    void protectedEndpoints_ShouldDenyAnonymous() throws Exception {
        // /profile and /feed require authentication
        mockMvc.perform(get("http://localhost:8081/feed"))
                .andExpect(status().isFound()); // Redirect to login (302)
        mockMvc.perform(get("http://localhost:8081/profile"))
                .andExpect(status().isFound());
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    @DisplayName("Authenticated user should access protected endpoints")
    void authenticatedUser_ShouldAccessProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/feed"))
                .andExpect(status().isOk());
        // Actual /feed may return 404 if not implemented; but we test security only.
        // If FeedView exists, it returns 200. For now, we check that it's not 302.
        // We can relax: status is either 200 or 404 but not redirect.
    }
}
