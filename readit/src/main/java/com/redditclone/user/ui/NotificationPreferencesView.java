package com.redditclone.user.ui;

import com.redditclone.user.domain.NotificationPreference;
import com.redditclone.user.dto.NotificationPreferenceDto;
import com.redditclone.user.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.beans.factory.annotation.Autowired;

@Route("settings/notifications")
@PageTitle("Notification Preferences | Reddit Clone")
@UIScope
public class NotificationPreferencesView extends VerticalLayout implements BeforeEnterObserver {

    @Autowired
    private UserService userService;

    private NotificationPreferenceDto currentPreferences;
    private Long currentUserId;

    private final Checkbox emailEnabled = new Checkbox("Email Notifications");
    private final Checkbox pushEnabled = new Checkbox("Push Notifications");
    private final Checkbox replyNotifications = new Checkbox("Replies to my posts/comments");
    private final Checkbox mentionNotifications = new Checkbox("Mentions (@username)");
    private final Checkbox voteNotifications = new Checkbox("Votes on my content");
    private final Checkbox moderationNotifications = new Checkbox("Moderation actions");
    private final ComboBox<String> emailFrequency = new ComboBox<>("Email Frequency");

    private final Button saveButton = new Button("Save Preferences");

    public NotificationPreferencesView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);

        // Setup email frequency dropdown
        emailFrequency.setItems("IMMEDIATELY", "DAILY", "WEEKLY");
        emailFrequency.setValue("IMMEDIATELY");

        // Build form
        FormLayout form = new FormLayout();
        form.add(
                emailEnabled,
                pushEnabled,
                replyNotifications,
                mentionNotifications,
                voteNotifications,
                moderationNotifications,
                emailFrequency,
                saveButton
        );
        form.setMaxWidth("500px");

        saveButton.addClickListener(e -> savePreferences());

        RouterLink backToProfile = new RouterLink("← Back to Profile", ProfileView.class, "");

        add(
                new H2("Notification Preferences"),
                new Paragraph("Control how and when you receive notifications."),
                form,
                backToProfile
        );
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        try {
            currentUserId = userService.getCurrentUser().getId();
            loadPreferences();
            populateForm();
        } catch (IllegalStateException e) {
            // Not logged in – redirect to login
            event.forwardTo(LoginView.class);
        }
    }

    private void loadPreferences() {
        NotificationPreference prefs = userService.getNotificationPreferences(currentUserId);
        currentPreferences = NotificationPreferenceDto.builder()
                .id(prefs.getId())
                .emailEnabled(prefs.isEmailEnabled())
                .pushEnabled(prefs.isPushEnabled())
                .replyNotifications(prefs.isReplyNotifications())
                .mentionNotifications(prefs.isMentionNotifications())
                .voteNotifications(prefs.isVoteNotifications())
                .moderationNotifications(prefs.isModerationNotifications())
                .emailFrequency(prefs.getEmailFrequency())
                .build();
    }

    private void populateForm() {
        if (currentPreferences == null) {
            return;
        }
        emailEnabled.setValue(currentPreferences.isEmailEnabled());
        pushEnabled.setValue(currentPreferences.isPushEnabled());
        replyNotifications.setValue(currentPreferences.isReplyNotifications());
        mentionNotifications.setValue(currentPreferences.isMentionNotifications());
        voteNotifications.setValue(currentPreferences.isVoteNotifications());
        moderationNotifications.setValue(currentPreferences.isModerationNotifications());
        emailFrequency.setValue(currentPreferences.getEmailFrequency());
    }

    private void savePreferences() {
        try {
            NotificationPreference updated = userService.updateNotificationPreferences(
                    currentUserId,
                    emailEnabled.getValue(),
                    pushEnabled.getValue(),
                    replyNotifications.getValue(),
                    mentionNotifications.getValue(),
                    voteNotifications.getValue(),
                    moderationNotifications.getValue(),
                    emailFrequency.getValue()
            );

            // Update local DTO
            currentPreferences.setEmailEnabled(updated.isEmailEnabled());
            currentPreferences.setPushEnabled(updated.isPushEnabled());
            currentPreferences.setReplyNotifications(updated.isReplyNotifications());
            currentPreferences.setMentionNotifications(updated.isMentionNotifications());
            currentPreferences.setVoteNotifications(updated.isVoteNotifications());
            currentPreferences.setModerationNotifications(updated.isModerationNotifications());
            currentPreferences.setEmailFrequency(updated.getEmailFrequency());

            Notification.show("Preferences saved successfully!", 3000, Notification.Position.MIDDLE);
        } catch (Exception e) {
            Notification.show("Error saving preferences: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }
}