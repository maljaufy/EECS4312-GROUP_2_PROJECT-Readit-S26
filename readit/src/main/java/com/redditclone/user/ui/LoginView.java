package com.redditclone.user.ui;

import com.redditclone.shared.security.AuthenticationSessionService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;

@Route("")
@RouteAlias("login")
@PageTitle("Login | Reddit Clone")
public class LoginView extends Composite<VerticalLayout>{
    /*
    Login view: Main login view: User login view
    i.e. Login UI
    */

    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password");

    @Autowired
    private AuthenticationSessionService authenticationSessionService;

    public LoginView() {
        getContent().setSizeFull();
        getContent().setAlignItems(Alignment.CENTER);
        getContent().setJustifyContentMode(JustifyContentMode.CENTER);
        getContent().addClassName("auth-view");
        getContent().getStyle()
            .set("background", "linear-gradient(135deg, #fff7ed 0%, #eff6ff 100%)")
            .set("padding", "20px");

        // Create card container
        VerticalLayout card = new VerticalLayout();
        card.setWidth("400px");
        card.setMaxWidth("calc(100vw - 40px)");
        card.setPadding(true);
        card.setSpacing(true);
        card.getStyle()
            .set("background", "white")
            .set("border-radius", "16px")
            .set("box-shadow", "0 8px 32px rgba(0, 0, 0, 0.1)")
            .set("padding", "40px");

        // Logo/Icon
        H2 logo = new H2("📱 Readit");
        logo.getStyle()
            .set("margin", "0 0 10px 0")
            .set("text-align", "center")
            .set("color", "#ff4500");

        H1 title = new H1("Welcome Back");
        title.getStyle()
            .set("margin", "0 0 8px 0")
            .set("text-align", "center")
            .set("font-size", "28px")
            .set("font-weight", "600")
            .set("color", "#111827");

        Paragraph subtitle = new Paragraph("Log in to continue to Readit");
        subtitle.getStyle()
            .set("margin", "0 0 30px 0")
            .set("text-align", "center")
            .set("color", "#666");

        username.setRequired(true);
        username.setWidthFull();
        username.setPrefixComponent(VaadinIcon.USER.create());
        username.getStyle().set("margin-bottom", "16px");

        password.setRequired(true);
        password.setWidthFull();
        password.setPrefixComponent(VaadinIcon.LOCK.create());
        password.getStyle().set("margin-bottom", "24px");

        Button loginButton = new Button("Log In");
        loginButton.setWidthFull();
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.getStyle()
            .set("background", "#ff4500")
            .set("font-weight", "600")
            .set("padding", "12px")
            .set("margin-bottom", "16px");
        loginButton.addClickListener(e -> handleLogin());

        RouterLink registerLink = new RouterLink("New user? Create an account", RegisterView.class);
        registerLink.getStyle()
            .set("text-align", "center")
            .set("display", "block")
            .set("color", "#006fbb")
            .set("text-decoration", "none")
            .set("margin-top", "8px");

        card.add(logo, title, subtitle, username, password, loginButton, registerLink);
        getContent().add(card);
    }

    private void handleLogin() {
        String user = username.getValue().trim();
        String pass = password.getValue();

        if (user.isEmpty() || pass.isEmpty()) {
            Notification.show("Please enter both username and password", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            getUI().ifPresent(ui -> {
                authenticationSessionService.signIn(ui, user, pass);
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
