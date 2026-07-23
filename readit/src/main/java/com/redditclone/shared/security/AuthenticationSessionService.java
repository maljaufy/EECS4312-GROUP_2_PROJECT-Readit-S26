package com.redditclone.shared.security;

import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import com.vaadin.flow.component.UI;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationSessionService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final UserSession userSession;

    public User signIn(UI ui, String username, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetails principal = (UserDetails) authentication.getPrincipal();
        String token = jwtUtil.generateToken(principal);
        User user = userService.findByUsername(username);
        userSession.establish(ui, token, user);
        return user;
    }
}
