package com.redditclone.user.ui;

import com.redditclone.shared.security.UserSession;
import com.redditclone.shared.ui.MainLayout;
import com.redditclone.user.dto.UserProfileDto;
import com.redditclone.user.service.UserService;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
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
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;

@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profile | Reddit Clone")
@UIScope
public class ProfileView extends Composite<VerticalLayout> implements HasUrlParameter<String> {

    @Autowired
    private UserService userService;

    @Autowired
    private UserSession userSession;

    private UserProfileDto currentProfile;
    private String viewedUsername;
    private boolean isOwnProfile;

    private Span karmaValue;
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
        karmaValue.getStyle().set("font-weight", "700");
        if (karma > 0) {
            karmaValue.getStyle().set("color", "#2e7d32");
        } else if (karma < 0) {
            karmaValue.getStyle().set("color", "#c62828");
        } else {
            karmaValue.getStyle().set("color", "#7c7c7c");
        }
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String username) {
        String currentUsername;
        try {
            currentUsername = userService.getCurrentUser().getUsername();
        } catch (IllegalStateException e) {
            currentUsername = null;
        }

        if (username == null || username.isEmpty()) {
            if (currentUsername == null) {
                getUI().ifPresent(ui -> ui.navigate("login"));
                return;
            }
            this.viewedUsername = currentUsername;
        } else {
            this.viewedUsername = username;
        }

        this.isOwnProfile = currentUsername != null && currentUsername.equals(viewedUsername);

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
        getContent().setPadding(false);
        getContent().setSpacing(false);
        getContent().getStyle()
                .set("background", "#DAE0E6")
                .set("padding", "32px 20px")
                .set("box-sizing", "border-box");

        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setMaxWidth("720px");
        mainContainer.setWidthFull();
        mainContainer.setPadding(false);
        mainContainer.setSpacing(false);

        mainContainer.add(
                createBanner(),
                createStatsCard(),
                createBioCard(),
                createNavigationLinks()
        );

        getContent().add(mainContainer);
    }

    /** Reddit-style profile banner: gradient, avatar, username, karma. */
    private Div createBanner() {
        Div banner = new Div();
        banner.getStyle()
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("background", "linear-gradient(135deg, #0079D3 0%, #0057A3 100%)")
                .set("border-radius", "8px 8px 0 0")
                .set("padding", "32px 24px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "20px")
                .set("flex-wrap", "wrap");

        Avatar avatar = new Avatar(currentProfile.getUsername());
        if (currentProfile.getProfileImageUrl() != null && !currentProfile.getProfileImageUrl().isEmpty()) {
            avatar.setImage(currentProfile.getProfileImageUrl());
        }
        avatar.setWidth("84px");
        avatar.setHeight("84px");
        avatar.getStyle()
                .set("border", "4px solid white")
                .set("box-shadow", "0 2px 6px rgba(0,0,0,0.2)");

        VerticalLayout userInfo = new VerticalLayout();
        userInfo.setSpacing(false);
        userInfo.setPadding(false);
        userInfo.getStyle().set("flex-grow", "1");

        H1 usernameHeader = new H1("u/" + currentProfile.getUsername());
        usernameHeader.getStyle()
                .set("margin", "0")
                .set("color", "white")
                .set("font-size", "26px")
                .set("font-weight", "700")
                .set("text-shadow", "0 1px 3px rgba(0,0,0,0.3)");

        userInfo.add(usernameHeader, createKarmaDisplay());

        banner.add(avatar, userInfo);

        if (isOwnProfile) {
            editToggleButton = new Button("Edit Profile", e -> toggleEdit());
            editToggleButton.getStyle()
                    .set("background", "white")
                    .set("color", "#0079D3")
                    .set("font-weight", "700")
                    .set("border-radius", "20px")
                    .set("padding", "8px 20px")
                    .set("flex-shrink", "0");
            banner.add(editToggleButton);
        }

        return banner;
    }

    private HorizontalLayout createKarmaDisplay() {
        HorizontalLayout display = new HorizontalLayout();
        display.setSpacing(true);
        display.setAlignItems(Alignment.CENTER);
        display.getStyle().set("margin-top", "4px");

        Icon karmaIcon = VaadinIcon.STAR.create();
        karmaIcon.getStyle().set("color", "#FFD700");
        karmaIcon.setSize("16px");

        karmaValue = new Span();
        updateKarmaDisplay();

        Span label = new Span("karma");
        label.getStyle()
                .set("color", "rgba(255,255,255,0.85)")
                .set("font-size", "13px");

        display.add(karmaIcon, karmaValue, label);
        return display;
    }

    private Div createStatsCard() {
        Div card = new Div();
        card.getStyle()
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("background", "white")
                .set("border", "1px solid #ccc")
                .set("border-top", "none")
                .set("padding", "16px 24px")
                .set("display", "flex")
                .set("gap", "32px")
                .set("flex-wrap", "wrap");

        card.add(
                statItem("Joined", currentProfile.getJoinedAt().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))),
                statItem("Posts", String.valueOf(currentProfile.getPostCount())),
                statItem("Comments", String.valueOf(currentProfile.getCommentCount()))
        );

        return card;
    }

    private VerticalLayout statItem(String label, String value) {
        VerticalLayout item = new VerticalLayout();
        item.setSpacing(false);
        item.setPadding(false);

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("font-weight", "700")
                .set("font-size", "15px")
                .set("color", "#1c1c1c");

        Span labelSpan = new Span(label);
        labelSpan.getStyle()
                .set("font-size", "12px")
                .set("color", "#7c7c7c")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px");

        item.add(valueSpan, labelSpan);
        return item;
    }

    private Div createBioCard() {
        Div card = new Div();
        card.getStyle()
                .set("width", "100%")
                .set("box-sizing", "border-box")
                .set("background", "white")
                .set("border", "1px solid #ccc")
                .set("border-top", "none")
                .set("border-radius", "0 0 8px 8px")
                .set("padding", "20px 24px")
                .set("margin-bottom", "16px");

        H3 bioHeader = new H3("About");
        bioHeader.getStyle()
                .set("margin", "0 0 12px 0")
                .set("font-size", "14px")
                .set("font-weight", "700")
                .set("color", "#1c1c1c")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px");

        bioDisplay = new TextArea();
        bioDisplay.setValue(currentProfile.getBio() != null ? currentProfile.getBio() : "");
        bioDisplay.setPlaceholder("This user hasn't written a bio yet.");
        bioDisplay.setReadOnly(true);
        bioDisplay.setWidthFull();

        bioEdit = new TextArea();
        bioEdit.setLabel("Edit Bio");
        bioEdit.setValue(currentProfile.getBio() != null ? currentProfile.getBio() : "");
        bioEdit.setWidthFull();
        bioEdit.setVisible(false);

        card.add(bioHeader, bioDisplay, bioEdit);

        if (isOwnProfile) {
            saveButton = new Button("Save", e -> saveProfile());
            saveButton.getStyle()
                    .set("background", "#0079D3")
                    .set("color", "white")
                    .set("font-weight", "700")
                    .set("border-radius", "20px")
                    .set("margin-top", "12px");
            saveButton.setVisible(false);
            card.add(saveButton);
        }

        return card;
    }

    private HorizontalLayout createNavigationLinks() {
        HorizontalLayout nav = new HorizontalLayout();
        nav.setWidthFull();
        nav.setJustifyContentMode(JustifyContentMode.CENTER);
        nav.setSpacing(true);
        nav.getStyle().set("margin-top", "16px").set("flex-wrap", "wrap");

        RouterLink backToFeed = new RouterLink("\u2190 Back to Feed", com.redditclone.posts.ui.FeedView.class);
        styleNavLink(backToFeed);
        nav.add(backToFeed);

        // Only the profile owner can see/manage their own notification settings.
        if (isOwnProfile) {
            RouterLink preferencesLink = new RouterLink("Notification Preferences", NotificationPreferencesView.class);
            styleNavLink(preferencesLink);
            nav.add(preferencesLink);
        }

        return nav;
    }

    private void styleNavLink(RouterLink link) {
        link.getStyle()
                .set("color", "#0079D3")
                .set("font-weight", "600")
                .set("font-size", "14px")
                .set("text-decoration", "none");
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
            renderProfile(); // Re-render with fresh data
            Notification.show("Profile updated successfully!", 3000, Notification.Position.MIDDLE);
        } catch (Exception e) {
            Notification.show("Error updating profile: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
}