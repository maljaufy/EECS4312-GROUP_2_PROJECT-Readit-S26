package com.redditclone.user.ui;

import com.redditclone.user.dto.UserProfileDto;
import com.redditclone.user.service.UserService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.format.DateTimeFormatter;

@Route("profile")
@PageTitle("Profile | Reddit Clone")
@UIScope
public class ProfileView extends Composite<VerticalLayout> implements HasUrlParameter<String>{
    /*
    Profile view: Main profile view: User profile view
    i.e. Profile UI
    */

    @Autowired
    private UserService userService;

    private UserProfileDto currentProfile;
    private String viewedUsername;

    private Avatar avatar;
    private H1 usernameHeader;
    private Span karmaValue;
    private Paragraph joinedDate;
    private Paragraph postCount;
    private Paragraph commentCount;
    private TextArea bioDisplay;
    private TextArea bioEdit;
    private Button editToggleButton;
    private Button saveButton;
    private boolean isEditing = false;

    /**
     * Listens for karma updates and refreshes the display.
     * This is triggered by VoteEventListener broadcasting.
     */
    public void onKarmaUpdated(String username) {
        if (viewedUsername != null && viewedUsername.equals(username)) {
            getUI().ifPresent(ui -> ui.access(() -> {
                loadProfile();
                updateKarmaDisplay();
            }));
        }
    }

    private void updateKarmaDisplay() {
        int karma = currentProfile.getKarma();
        karmaValue.setText(String.valueOf(karma));
        karmaValue.getStyle().set("font-weight", karma == 0 ? "normal" : "bold");
        if (karma > 0) {
            karmaValue.getStyle().set("color", "var(--lumo-success-color)");
        } else if (karma < 0) {
            karmaValue.getStyle().set("color", "var(--lumo-error-color)");
        } else {
            karmaValue.getStyle().set("color", "var(--lumo-secondary-text-color)");
        }
    }

    @Override
    public void setParameter(BeforeEvent event, String username) {
        if (username == null || username.isEmpty()) {
            try {
                this.viewedUsername = userService.getCurrentUser().getUsername();
            } catch (IllegalStateException e) {
                getUI().ifPresent(ui -> ui.navigate("login"));
                return;
            }
        } else {
            this.viewedUsername = username;
        }
        loadProfile();
        renderProfile();
    }

    private void loadProfile() {
        currentProfile = userService.getUserProfile(viewedUsername);
    }

    private void renderProfile() {
        getContent().removeAll();
        getContent().setSizeFull();
        getContent().setAlignItems(Alignment.CENTER);
        getContent().setJustifyContentMode(JustifyContentMode.CENTER);
        getContent().getStyle()
            .set("background", "linear-gradient(135deg, #556B2F 0%, #8B7355 100%)")
            .set("padding", "40px 20px");

        // Main container
        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setMaxWidth("800px");
        mainContainer.setWidthFull();
        mainContainer.setPadding(false);
        mainContainer.setSpacing(true);

        // Header with logout button
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.getStyle().set("margin-bottom", "20px");

        H2 logo = new H2("📱 Readit");
        logo.getStyle()
            .set("margin", "0")
            .set("color", "#F5DEB3");

        Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        logoutButton.getStyle()
            .set("background", "#D2B48C")
            .set("color", "#333")
            .set("font-weight", "600")
            .set("padding", "8px 16px");
        logoutButton.addClickListener(e -> handleLogout());

        headerLayout.add(logo, logoutButton);
        mainContainer.add(headerLayout);

        // Header with avatar and username
        HorizontalLayout header = new HorizontalLayout();
        header.setAlignItems(Alignment.CENTER);
        header.setSpacing(true);

        avatar = new Avatar(currentProfile.getUsername());
        if (currentProfile.getProfileImageUrl() != null && !currentProfile.getProfileImageUrl().isEmpty()) {
            avatar.setImage(currentProfile.getProfileImageUrl());
        }
        avatar.setWidth("80px");
        avatar.setHeight("80px");

        VerticalLayout userInfo = new VerticalLayout();
        usernameHeader = new H1(currentProfile.getUsername());
        usernameHeader.getStyle()
            .set("margin", "0")
            .set("color", "#F5DEB3");

        userInfo.add(usernameHeader, createKarmaDisplay());

        header.add(avatar, userInfo);
        mainContainer.add(header);

        // Stats
        HorizontalLayout stats = new HorizontalLayout();
        stats.setSpacing(true);

        joinedDate = new Paragraph("Joined: " + currentProfile.getJoinedAt().format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        joinedDate.getStyle().set("color", "var(--lumo-secondary-text-color)");

        postCount = new Paragraph("Posts: " + currentProfile.getPostCount());
        commentCount = new Paragraph("Comments: " + currentProfile.getCommentCount());

        stats.add(joinedDate, postCount, commentCount);
        mainContainer.add(stats);

        // Bio
        bioDisplay = new TextArea();
        bioDisplay.setLabel("Bio");
        bioDisplay.setValue(currentProfile.getBio() != null ? currentProfile.getBio() : "");
        bioDisplay.setReadOnly(true);
        bioDisplay.setWidthFull();

        bioEdit = new TextArea();
        bioEdit.setLabel("Edit Bio");
        bioEdit.setValue(currentProfile.getBio() != null ? currentProfile.getBio() : "");
        bioEdit.setWidthFull();
        bioEdit.setVisible(false);

        mainContainer.add(bioDisplay, bioEdit);

        // Edit buttons (only if viewing own profile)
        try {
            String currentUsername = userService.getCurrentUser().getUsername();
            if (currentUsername.equals(viewedUsername)) {
                editToggleButton = new Button("Edit Profile", e -> toggleEdit());
                saveButton = new Button("Save", e -> saveProfile());
                saveButton.setVisible(false);
                HorizontalLayout buttonLayout = new HorizontalLayout(editToggleButton, saveButton);
                mainContainer.add(buttonLayout);
            }
        } catch (IllegalStateException e) {
            // Not logged in - no edit buttons
        }

        // Navigation
        RouterLink backToFeed = new RouterLink("← Back to Feed", com.redditclone.posts.ui.FeedView.class);
        backToFeed.getStyle().set("color", "#F5DEB3");
        mainContainer.add(backToFeed);

        getContent().add(mainContainer);
    }

    private HorizontalLayout createKarmaDisplay() {
        HorizontalLayout display = new HorizontalLayout();
        display.setSpacing(true);
        display.setAlignItems(Alignment.CENTER);

        Icon karmaIcon = VaadinIcon.STAR.create();
        karmaIcon.getStyle().set("color", "var(--lumo-primary-color)");
        karmaIcon.setSize("16px");

        karmaValue = new Span();
        updateKarmaDisplay();
        display.add(karmaIcon, karmaValue);
        return display;
    }

    private void toggleEdit() {
        isEditing = !isEditing;
        bioDisplay.setVisible(!isEditing);
        bioEdit.setVisible(isEditing);
        editToggleButton.setText(isEditing ? "Cancel" : "Edit Profile");
        saveButton.setVisible(isEditing);
    }

    private void saveProfile() {
        try {
            UserProfileDto updated = userService.updateProfile(
                    userService.getCurrentUser().getId(),
                    bioEdit.getValue(),
                    null // profileImageUrl not implemented yet
            );
            currentProfile = updated;
            loadProfile(); // Reload
            toggleEdit(); // Exit edit mode
            Notification.show("Profile updated successfully!", 3000, Notification.Position.MIDDLE);
        } catch (Exception e) {
            Notification.show("Error updating profile: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void handleLogout() {
        SecurityContextHolder.clearContext();
        getUI().ifPresent(ui -> {
            ui.getSession().setAttribute("jwt", null);
            ui.getSession().setAttribute("username", null);
            ui.navigate("");
        });
    }
}
