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
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle()
            .set("background", "linear-gradient(135deg, #556B2F 0%, #8B7355 100%)")
            .set("padding", "40px 20px");

        // Main container
        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setMaxWidth("700px");
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

        H2 pageTitle = new H2("Create a post");
        pageTitle.getStyle().set("color", "#F5DEB3");

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
                author = userService.getCurrentUser();
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
        submit.getStyle().set("background", "#8B7355");

        Button cancel = new Button("Cancel", e -> getUI().ifPresent(ui -> ui.navigate("feed")));

        mainContainer.add(headerLayout, pageTitle);
        if (subreddits.isEmpty()) {
            mainContainer.add(new Paragraph("There are no subreddits yet. Create one first, then come back."));
        }
        mainContainer.add(title, content, subreddit, new HorizontalLayout(submit, cancel));
        add(mainContainer);
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
