package com.redditclone.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JWT refresh authentication tests")
class JwtAuthenticationFilterSessionTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private CustomUserDetailsService userDetailsService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authenticates a refreshed Vaadin route from the HTTP session token")
    void authenticatesFromHttpSession() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, userDetailsService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/feed");
        request.getSession().setAttribute(UserSession.JWT_ATTRIBUTE, "signed-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserDetails details = User.withUsername("new-user").password("unused").roles("USER").build();

        when(jwtUtil.extractUsername("signed-token")).thenReturn("new-user");
        when(userDetailsService.loadUserByUsername("new-user")).thenReturn(details);
        when(jwtUtil.validateToken("signed-token", details)).thenReturn(true);

        filter.doFilter(request, response, new MockFilterChain());

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("new-user", SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
