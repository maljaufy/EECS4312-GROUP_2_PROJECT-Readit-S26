package com.redditclone.posts.ui;

import com.redditclone.posts.dto.PostDto;
import com.redditclone.posts.service.PostService;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.service.SubredditService;
import com.redditclone.user.domain.User;
import com.redditclone.user.service.UserService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Route("create-post")
@PageTitle("Create Post | Reddit Clone")
public class CreatePostView extends VerticalLayout {

    public CreatePostView(PostService postService,
                          SubredditService subredditService,
                          UserService userService) {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.START);
        addClassName("creation-view");
        getStyle()
            .set("background", "#f3f4f6")
            .set("overflow-y", "auto")
            .set("padding", "32px 20px");

        // Main container
        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setMaxWidth("760px");
        mainContainer.setWidthFull();
        mainContainer.setPadding(true);
        mainContainer.setSpacing(true);
        mainContainer.addClassName("creation-card");
        mainContainer.getStyle()
            .set("background", "#ffffff")
            .set("border", "1px solid #d7dce2")
            .set("border-radius", "14px")
            .set("box-shadow", "0 10px 30px rgba(15, 23, 42, 0.08)")
            .set("padding", "28px");

        // Header with logout button
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.getStyle().set("margin-bottom", "20px");

        H2 logo = new H2("📱 Readit");
        logo.getStyle()
            .set("margin", "0")
            .set("color", "#ff4500")
            .set("cursor", "pointer");
        logo.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("feed")));

        Button logoutButton = new Button("Logout", VaadinIcon.SIGN_OUT.create());
        logoutButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        logoutButton.getStyle()
            .set("color", "#334155")
            .set("font-weight", "600")
            .set("padding", "8px 16px");
        logoutButton.addClickListener(e -> handleLogout());

        headerLayout.add(logo, logoutButton);

        H2 pageTitle = new H2("Create a post");
        pageTitle.getStyle()
            .set("color", "#111827")
            .set("margin", "4px 0 0");

        Paragraph pageDescription = new Paragraph("Share something with a community and start a conversation.");
        pageDescription.getStyle()
            .set("color", "#52606d")
            .set("margin", "-8px 0 8px");

        TextField title = new TextField("Title");
        title.setRequiredIndicatorVisible(true);
        title.setMaxLength(300);
        title.setWidthFull();

        TextArea content = new TextArea("Content");
        content.setWidthFull();
        content.setMinHeight("160px");

        ComboBox<Subreddit> subreddit = new ComboBox<>("Subreddit");
        List<Subreddit> subreddits = subredditService.findAll();
        subreddit.setItems(subreddits);
        subreddit.setItemLabelGenerator(Subreddit::getName);
        subreddit.setRequiredIndicatorVisible(true);
        subreddit.setWidthFull();
        subreddit.setAllowCustomValue(false);
        subreddit.setPlaceholder("Select a subreddit");

        Button submit = new Button("Submit post", event -> {
            User author;
            try {
                author = resolveCurrentUser(userService);
            } catch (IllegalStateException notLoggedIn) {
                Notification error = Notification.show("Please log in first.");
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                getUI().ifPresent(ui -> ui.navigate("login"));
                return;
            }
            if (title.getValue() == null || title.getValue().trim().isEmpty()) {
                Notification error = Notification.show("Please enter a title.");
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (content.getValue() == null || content.getValue().trim().isEmpty()) {
                Notification error = Notification.show("Please enter content.");
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            if (subreddit.getValue() == null) {
                Notification error = Notification.show("Please choose a subreddit.");
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                PostDto dto = new PostDto(
                        title.getValue(),
                        content.getValue(),
                        subreddit.getValue().getId());
                postService.createPost(dto, author);
                Notification ok = Notification.show("Post created.");
                ok.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                getUI().ifPresent(ui -> ui.navigate("feed"));
            } catch (IllegalArgumentException ex) {
                Notification error = Notification.show(ex.getMessage());
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                Notification error = Notification.show("Error creating post: " + ex.getMessage());
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                ex.printStackTrace();
            }
        });
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        submit.getStyle()
            .set("background", "#ff4500")
            .set("font-weight", "700");

        Button cancel = new Button("Cancel", e -> getUI().ifPresent(ui -> ui.navigate("feed")));
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout actions = new HorizontalLayout(cancel, submit);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.END);

        mainContainer.add(headerLayout, pageTitle, pageDescription);
        if (subreddits.isEmpty()) {
            Div emptyCommunityNotice = new Div();
            emptyCommunityNotice.getStyle()
                .set("background", "#fff7ed")
                .set("border", "1px solid #fed7aa")
                .set("border-radius", "10px")
                .set("color", "#7c2d12")
                .set("padding", "14px 16px");
            Paragraph noticeText = new Paragraph(
                    "There are no communities yet. Create one before publishing your first post.");
            noticeText.getStyle().set("margin", "0 0 10px");
            Button createCommunity = new Button("Create a community",
                    e -> getUI().ifPresent(ui -> ui.navigate("create-subreddit")));
            createCommunity.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            emptyCommunityNotice.add(noticeText, createCommunity);
            mainContainer.add(emptyCommunityNotice);
            subreddit.setEnabled(false);
            submit.setEnabled(false);
        }
        mainContainer.add(title, content, subreddit, actions);
        add(mainContainer);
    }

    private User resolveCurrentUser(UserService userService) {
        Long sessionUserId = getUI()
                .map(ui -> (Long) ui.getSession().getAttribute("userId"))
                .orElse(null);
        return sessionUserId == null ? userService.getCurrentUser() : userService.findById(sessionUserId);
    }

    private void handleLogout() {
        SecurityContextHolder.clearContext();
        getUI().ifPresent(ui -> {
            ui.getSession().setAttribute("jwt", null);
            ui.getSession().setAttribute("username", null);
            ui.getSession().setAttribute("userId", null);
            ui.navigate("");
        });
    }
}
