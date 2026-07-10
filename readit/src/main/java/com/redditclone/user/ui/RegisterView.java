package com.redditclone.user.ui;

import com.redditclone.user.dto.RegistrationDto;
import com.redditclone.user.service.UserService;
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
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;

@Route("register")
@PageTitle("Register | Reddit Clone")
public class RegisterView extends Composite<VerticalLayout>{

    /*
    Register view: Main register view: User registration view
    i.e. Registration UI
    */

    private final TextField username = new TextField("Username");
    private final EmailField email = new EmailField("Email");
    private final PasswordField password = new PasswordField("Password");
    private final PasswordField confirmPassword = new PasswordField("Confirm Password");
    private final Button registerButton = new Button("Create Account");
    private final RouterLink loginLink = new RouterLink("Already have an account? Log in", LoginView.class);

    @Autowired
    private UserService userService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    public RegisterView() {
        getContent().setSizeFull();
        getContent().setAlignItems(Alignment.CENTER);
        getContent().setJustifyContentMode(JustifyContentMode.CENTER);
        getContent().getStyle()
            .set("background", "linear-gradient(135deg, #556B2F 0%, #8B7355 100%)")
            .set("padding", "20px");

        // Create card container
        VerticalLayout card = new VerticalLayout();
        card.setWidth("450px");
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
            .set("color", "#F5DEB3");

        H1 title = new H1("Join Readit");
        title.getStyle()
            .set("margin", "0 0 8px 0")
            .set("text-align", "center")
            .set("font-size", "28px")
            .set("font-weight", "600")
            .set("color", "#F5DEB3");

        Paragraph subtitle = new Paragraph("Create your account to start exploring communities");
        subtitle.getStyle()
            .set("margin", "0 0 30px 0")
            .set("text-align", "center")
            .set("color", "#666");

        username.setRequired(true);
        username.setMinLength(3);
        username.setWidthFull();
        username.setMaxLength(50);
        username.setPrefixComponent(VaadinIcon.USER.create());
        username.setHelperText("3-50 characters");
        username.setErrorMessage("Please enter a valid username");
        username.getStyle().set("margin-bottom", "12px");

        email.setRequired(true);
        email.setErrorMessage("Please enter a valid email address");
        email.setWidthFull();
        email.setMaxLength(100);
        email.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        email.setHelperText("Enter your email address");
        email.setErrorMessage("Please enter a valid email address");
        email.getStyle().set("margin-bottom", "12px");

        password.setRequired(true);
        password.setMinLength(8);
        password.setHelperText("At least 8 characters");
        password.setWidthFull();
        password.setMaxLength(100);
        password.setPrefixComponent(VaadinIcon.LOCK.create());
        password.setHelperText("At least 8 characters");
        password.setErrorMessage("Please enter a valid password");
        password.getStyle().set("margin-bottom", "12px");

        confirmPassword.setRequired(true);
        confirmPassword.setWidthFull();
        confirmPassword.setMaxLength(100);
        confirmPassword.setPrefixComponent(VaadinIcon.LOCK.create());
        confirmPassword.setHelperText("Re-enter your password");
        confirmPassword.setErrorMessage("Please enter a valid password");
        confirmPassword.getStyle().set("margin-bottom", "20px");

        registerButton.addClickListener(e -> handleRegistration());
        registerButton.setWidthFull();
        registerButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerButton.getStyle()
            .set("background", "#8B7355")
            .set("font-weight", "600")
            .set("padding", "12px")
            .set("margin-bottom", "16px");

        loginLink.getStyle()
            .set("text-align", "center")
            .set("display", "block")
            .set("color", "#8B7355")
            .set("text-decoration", "none")
            .set("margin-top", "8px");

        card.add(logo, title, subtitle, username, email, password, confirmPassword, registerButton, loginLink);
        getContent().add(card);
    }

    private void handleRegistration() {
        String pass = password.getValue();
        String confirm = confirmPassword.getValue();

        // Validate passwords match
        if (!pass.equals(confirm)) {
            Notification.show("Passwords do not match", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Validate password length
        if (pass.length() < 8) {
            Notification.show("Password must be at least 8 characters", 3000, Notification.Position.MIDDLE);
            return;
        }

        RegistrationDto dto = new RegistrationDto();
        dto.setUsername(username.getValue().trim());
        dto.setEmail(email.getValue().trim());
        dto.setPassword(pass);
        dto.setConfirmPassword(confirm);

        // Validate with Bean Validation
        var violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String errorMessage = violations.iterator().next().getMessage();
            Notification.show(errorMessage, 5000, Notification.Position.MIDDLE);
            return;
        }

        try {
            userService.register(dto.getUsername(), dto.getEmail(), dto.getPassword());
            Notification.show(
                    "Registration successful! Welcome to Readit!",
                    3000,
                    Notification.Position.MIDDLE
            );
            getUI().ifPresent(ui -> ui.navigate("recommendations"));
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage(), 5000, Notification.Position.MIDDLE);
        } catch (Exception ex) {
            Notification.show("An unexpected error occurred. Please try again.", 5000, Notification.Position.MIDDLE);
        }
    }
}
