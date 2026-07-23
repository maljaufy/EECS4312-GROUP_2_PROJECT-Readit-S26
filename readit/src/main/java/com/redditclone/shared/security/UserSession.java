package com.redditclone.shared.security;

import com.redditclone.user.domain.User;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Keeps Vaadin UI state and the underlying HTTP session in sync. */
@Component
public class UserSession {

    public static final String JWT_ATTRIBUTE = "jwt";
    public static final String USERNAME_ATTRIBUTE = "username";
    public static final String USER_ID_ATTRIBUTE = "userId";

    public void establish(UI ui, String jwt, User user) {
        VaadinSession session = ui.getSession();
        session.setAttribute(JWT_ATTRIBUTE, jwt);
        session.setAttribute(USERNAME_ATTRIBUTE, user.getUsername());
        session.setAttribute(USER_ID_ATTRIBUTE, user.getId());

        // JwtAuthenticationFilter runs before Vaadin restores VaadinSession, so
        // it must be able to read the token from the wrapped HTTP session.
        session.getSession().setAttribute(JWT_ATTRIBUTE, jwt);
        session.getSession().setAttribute(USERNAME_ATTRIBUTE, user.getUsername());
        session.getSession().setAttribute(USER_ID_ATTRIBUTE, user.getId());
    }

    public Long currentUserId(UI ui) {
        Long userId = (Long) ui.getSession().getAttribute(USER_ID_ATTRIBUTE);
        if (userId == null) {
            userId = (Long) ui.getSession().getSession().getAttribute(USER_ID_ATTRIBUTE);
            if (userId != null) {
                ui.getSession().setAttribute(USER_ID_ATTRIBUTE, userId);
            }
        }
        return userId;
    }

    public void clear(UI ui) {
        SecurityContextHolder.clearContext();
        VaadinSession session = ui.getSession();
        session.setAttribute(JWT_ATTRIBUTE, null);
        session.setAttribute(USERNAME_ATTRIBUTE, null);
        session.setAttribute(USER_ID_ATTRIBUTE, null);
        session.getSession().removeAttribute(JWT_ATTRIBUTE);
        session.getSession().removeAttribute(USERNAME_ATTRIBUTE);
        session.getSession().removeAttribute(USER_ID_ATTRIBUTE);
    }
}
