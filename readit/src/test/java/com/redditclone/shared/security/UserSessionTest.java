package com.redditclone.shared.security;

import com.redditclone.user.domain.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Vaadin and HTTP user-session bridge tests")
class UserSessionTest {

    @Mock private UI ui;
    @Mock private VaadinSession vaadinSession;
    @Mock private WrappedSession httpSession;

    private UserSession userSession;

    @BeforeEach
    void setUp() {
        userSession = new UserSession();
        when(ui.getSession()).thenReturn(vaadinSession);
        when(vaadinSession.getSession()).thenReturn(httpSession);
    }

    @Test
    @DisplayName("Establishes identity in both session layers")
    void establishStoresBothSessionLayers() {
        User user = new User();
        user.setId(42L);
        user.setUsername("new-user");

        userSession.establish(ui, "signed-token", user);

        verify(vaadinSession).setAttribute(UserSession.JWT_ATTRIBUTE, "signed-token");
        verify(vaadinSession).setAttribute(UserSession.USERNAME_ATTRIBUTE, "new-user");
        verify(vaadinSession).setAttribute(UserSession.USER_ID_ATTRIBUTE, 42L);
        verify(httpSession).setAttribute(UserSession.JWT_ATTRIBUTE, "signed-token");
        verify(httpSession).setAttribute(UserSession.USERNAME_ATTRIBUTE, "new-user");
        verify(httpSession).setAttribute(UserSession.USER_ID_ATTRIBUTE, 42L);
    }

    @Test
    @DisplayName("Restores a user ID from HTTP session after UI refresh")
    void currentUserIdRestoresRefreshedUi() {
        when(vaadinSession.getAttribute(UserSession.USER_ID_ATTRIBUTE)).thenReturn(null);
        when(httpSession.getAttribute(UserSession.USER_ID_ATTRIBUTE)).thenReturn(42L);

        assertEquals(42L, userSession.currentUserId(ui));

        verify(vaadinSession).setAttribute(UserSession.USER_ID_ATTRIBUTE, 42L);
    }
}
