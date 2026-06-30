package com.redditclone.posts.ui;

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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route("create-post")
@PageTitle("Create Post | Reddit Clone")
public class CreatePostView extends VerticalLayout {

    public CreatePostView(PostService postService,
                          SubredditService subredditService,
                          UserService userService) {
        setPadding(true);
        setSpacing(true);
        setMaxWidth("700px");

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
            if (subreddit.getValue() == null) {
                Notification error = Notification.show("Please choose a subreddit.");
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            try {
                postService.createPost(
                        title.getValue(),
                        content.getValue(),
                        subreddit.getValue().getId(),
                        author);
                Notification ok = Notification.show("Post created.");
                ok.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                getUI().ifPresent(ui -> ui.navigate("feed"));
            } catch (IllegalArgumentException ex) {
                Notification error = Notification.show(ex.getMessage());
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        submit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button("Cancel", e -> getUI().ifPresent(ui -> ui.navigate("feed")));

        add(new H2("Create a post"));
        if (subreddits.isEmpty()) {
            add(new Paragraph("There are no subreddits yet. Create one first, then come back."));
        }
        add(title, content, subreddit, new HorizontalLayout(submit, cancel));
    }
}
