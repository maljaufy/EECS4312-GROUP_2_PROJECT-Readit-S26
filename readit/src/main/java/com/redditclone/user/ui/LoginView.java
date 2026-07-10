package com.redditclone.user.ui;

import com.redditclone.shared.security.JwtUtil;
import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("login")
@PageTitle("Login | Reddit Clone")
public class LoginView extends Composite<VerticalLayout>{
    /*
    Login view: Main login view: User login view
    i.e. Login UI
    */

    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password");

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    public LoginView() {
        getContent().setSizeFull();
        getContent().setAlignItems(Alignment.CENTER);
        getContent().setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Welcome to Back");
        title.getStyle().set("margin-bottom", "20px");

        Paragraph subtitle = new Paragraph("Log in to continue to Readit.");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");

        username.setRequired(true);
        username.setWidth("300px");
        password.setRequired(true);
        password.setWidth("300px");



        RouterLink registerLink = new RouterLink("New user? Create an account", RegisterView.class);

        Button loginButton = new Button("Log In");
        loginButton.setWidth("300px");
        //loginButton.setWidthFull();

        loginButton.addClickListener(e -> handleLogin());

        getContent().add(title, subtitle, username, password, loginButton, registerLink);
    }

    private void handleLogin() {
        String user = username.getValue().trim();
        String pass = password.getValue();

        if (user.isEmpty() || pass.isEmpty()) {
            Notification.show("Please enter both username and password", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user, pass)
            );

            SecurityContextHolder.getContext().setAuthentication(auth);

            // Generate JWT token
            String token = jwtUtil.generateToken(
                    (org.springframework.security.core.userdetails.User) auth.getPrincipal()
            );

            // Store token in Vaadin session
            User loggedInUser = userService.findByUsername(user); // fetch the real entity once, right here
            getUI().ifPresent(ui -> {
                ui.getSession().setAttribute("jwt", token);
                ui.getSession().setAttribute("username", user);
                ui.getSession().setAttribute("userId", loggedInUser.getId()); // <-- new
                ui.navigate("feed");
            });
            Notification.show("Welcome back, " + user + "!", 3000, Notification.Position.MIDDLE);

        } catch (BadCredentialsException e) {
            Notification.show("Invalid username or password", 5000, Notification.Position.MIDDLE);
        } catch (Exception e) {
            Notification.show("An error occurred: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

}
