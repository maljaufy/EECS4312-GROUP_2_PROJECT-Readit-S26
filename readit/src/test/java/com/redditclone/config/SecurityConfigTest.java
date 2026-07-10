package com.redditclone.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;

import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@DisplayName("Security Configuration Integration Tests")
public class SecurityConfigTest {
    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private MockMvc mockMvc;

    // Mock Vaadin views if they cause issues; but we can just test endpoints.
    // For endpoints that are not mapped, we get 404; but that's fine.

    @Test
    @DisplayName("Public endpoints should be accessible without authentication")
    void publicEndpoints_ShouldBeAccessible() throws Exception {
        // MockMvc has no named Vaadin servlet, so an allowed Vaadin route reaches
        // the forwarding controller and then throws. That proves security did not
        // reject the public request before the Vaadin handoff.
        assertVaadinHandoff("/register");
        assertVaadinHandoff("/");
    }

    @Test
    @DisplayName("Protected endpoints should be denied without authentication")
    void protectedEndpoints_ShouldDenyAnonymous() throws Exception {
        // This stateless security configuration responds with 403 rather than redirecting.
        mockMvc.perform(get("/feed"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/profile"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testUser", roles = "USER")
    @DisplayName("Authenticated user should access protected endpoints")
    void authenticatedUser_ShouldAccessProtectedEndpoints() throws Exception {
        assertVaadinHandoff("/feed");
    }

    private void assertVaadinHandoff(String path) {
        assertThrows(ServletException.class, () -> mockMvc.perform(get(path)));
    }
}
